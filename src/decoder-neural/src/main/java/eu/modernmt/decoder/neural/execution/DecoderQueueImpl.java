package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.decoder.neural.natv.NativeProcess;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by davide on 22/05/17.
 */
public class DecoderQueueImpl implements DecoderQueue {

    protected final Logger logger = LogManager.getLogger(getClass());

    private final int capacity;
    private final NativeProcess.Builder processBuilder;
    private final BlockingQueue<NativeProcess> queue;
    private final ExecutorService initExecutor;
    private final int[] gpus;
    private int idx;

    private final AtomicInteger aliveProcesses = new AtomicInteger(0);
    private boolean active = true;

    public DecoderQueueImpl(NativeProcess.Builder processBuilder, int[] gpus) throws DecoderException {
        this.capacity = gpus.length;
        this.processBuilder = processBuilder;
        this.queue = new ArrayBlockingQueue<>(gpus.length);
        this.initExecutor = gpus.length > 1 ? Executors.newCachedThreadPool() : Executors.newSingleThreadExecutor();
        this.gpus = gpus;
        this.idx = gpus.length - 1;

        this.init();
    }

    protected final DecoderQueueImpl init() throws DecoderException {
        Future<?>[] array = new Future<?>[capacity];
        for (int i = 0; i < capacity; i++)
            array[i] = this.initExecutor.submit(new Initializer());

        for (int i = 0; i < capacity; i++) {
            try {
                array[i].get();
            } catch (InterruptedException e) {
                throw new DecoderException("Initialization interrupted", e);
            } catch (ExecutionException e) {
                throw new DecoderException("Unexpected error during initialization", e.getCause());
            }
        }

        return this;
    }

    private NativeProcess getProcess() throws DecoderException {
        if (this.active && this.aliveProcesses.get() > 0) {
            try {
                return this.queue.take();
            } catch (InterruptedException e) {
                throw new DecoderUnavailableException("No NMT processes available", e);
            }
        } else {
            throw new DecoderUnavailableException("No alive NMT processes available");
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

    @Override
    public final Translation translate(LanguagePair direction, Sentence sentence, int nBest) throws DecoderException {
        NativeProcess process = getProcess();

        try {
            return process.translate(direction, sentence, nBest);
        } finally {
            releaseProcess(process);
        }
    }

    @Override
    public final Translation translate(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws DecoderException {
        NativeProcess process = getProcess();

        try {
            return process.translate(direction, sentence, suggestions, nBest);
        } finally {
            releaseProcess(process);
        }
    }

    private NativeProcess startProcess(NativeProcess.Builder processBuilder) throws IOException {
        int gpu;

        synchronized (this) {
            gpu = this.gpus[this.idx--];
        }

        return processBuilder.start(gpu);
    }

    private void onProcessDied(NativeProcess process) {
        synchronized (this) {
            this.gpus[++this.idx] = process.getGPU();
        }
    }

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
            } catch (IOException e) {
                logger.error("Failed to start new decoder process", e);
                System.exit(2);

                return;
            }

            queue.offer(process);
            aliveProcesses.incrementAndGet();
        }

    }
}
