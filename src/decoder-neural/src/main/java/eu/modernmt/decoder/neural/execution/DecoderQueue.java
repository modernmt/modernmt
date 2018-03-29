package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.neural.NeuralDecoderException;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.decoder.neural.natv.NativeProcess;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by davide on 22/05/17.
 */
public abstract class DecoderQueue implements Closeable {

    public static DecoderQueue newGPUInstance(File home, File model, int[] gpus) throws NeuralDecoderException {
        NativeProcess.Builder builder = new NativeProcess.Builder(home, model);
        return new GPUDecoderQueue(builder, gpus).init();
    }

    public static DecoderQueue newCPUInstance(File home, File model, int cpus) throws NeuralDecoderException {
        NativeProcess.Builder builder = new NativeProcess.Builder(home, model);
        return new CPUDecoderQueue(builder, cpus).init();
    }

    protected final Logger logger = LogManager.getLogger(getClass());

    private final int capacity;
    private final NativeProcess.Builder processBuilder;
    private final BlockingQueue<NativeProcess> queue;
    private final ExecutorService initExecutor;

    private final AtomicInteger aliveProcesses = new AtomicInteger(0);
    private boolean active = true;

    protected DecoderQueue(NativeProcess.Builder processBuilder, int capacity) {
        this.capacity = capacity;
        this.processBuilder = processBuilder;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.initExecutor = capacity > 1 ? Executors.newCachedThreadPool() : Executors.newSingleThreadExecutor();
    }

    protected final DecoderQueue init() throws NeuralDecoderException {
        Future<?>[] array = new Future<?>[capacity];
        for (int i = 0; i < capacity; i++)
            array[i] = this.initExecutor.submit(new Initializer());

        for (int i = 0; i < capacity; i++) {
            try {
                array[i].get();
            } catch (InterruptedException e) {
                throw new NeuralDecoderException("Initialization interrupted", e);
            } catch (ExecutionException e) {
                throw new NeuralDecoderException("Unexpected error during initialization", e.getCause());
            }
        }

        return this;
    }

    private NativeProcess getProcess() throws NeuralDecoderException {
        if (this.active && this.aliveProcesses.get() > 0) {
            try {
                return this.queue.take();
            } catch (InterruptedException e) {
                throw new NeuralDecoderException("No NMT processes available", e);
            }
        } else {
            throw new NeuralDecoderException("No alive NMT processes available");
        }
    }

    private void releaseProcess(NativeProcess process) {
        if (!this.active) {
            IOUtils.closeQuietly(process);
        } else {
            if (process.isAlive()) {
                this.queue.offer(process);
            } else {
                this.aliveProcesses.decrementAndGet();
                IOUtils.closeQuietly(process);

                this.onProcessDied(process);

                if (this.active)
                    this.initExecutor.execute(new Initializer());
            }
        }
    }

    public final Translation translate(LanguagePair direction, String variant, Sentence sentence, int nBest) throws NeuralDecoderException {
        NativeProcess process = getProcess();

        try {
            return process.translate(direction, variant, sentence, nBest);
        } finally {
            releaseProcess(process);
        }
    }

    public final Translation translate(LanguagePair direction, String variant, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws NeuralDecoderException {
        NativeProcess process = getProcess();

        try {
            return process.translate(direction, variant, sentence, suggestions, nBest);
        } finally {
            releaseProcess(process);
        }
    }

    protected abstract NativeProcess startProcess(NativeProcess.Builder processBuilder) throws NeuralDecoderException;

    protected abstract void onProcessDied(NativeProcess process);

    @Override
    public void close() {
        this.active = false;

        this.initExecutor.shutdownNow();
        try {
            this.initExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignore it
        }

        NativeProcess process;
        while ((process = this.queue.poll()) != null) {
            IOUtils.closeQuietly(process);
        }
    }

    private class Initializer implements Runnable {

        @Override
        public void run() {
            NativeProcess process;

            try {
                logger.info("Starting native decoder process");
                process = startProcess(processBuilder);
            } catch (NeuralDecoderException e) {
                logger.error("Failed to start new decoder process", e);
                System.exit(2);

                return;
            }

            queue.offer(process);
            aliveProcesses.incrementAndGet();
        }

    }
}
