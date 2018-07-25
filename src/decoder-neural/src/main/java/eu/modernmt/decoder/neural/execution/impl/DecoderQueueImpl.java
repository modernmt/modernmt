package eu.modernmt.decoder.neural.execution.impl;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderListener;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.decoder.neural.execution.DecoderQueue;
import eu.modernmt.decoder.neural.execution.PythonDecoder;
import eu.modernmt.lang.LanguagePair;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by davide on 22/05/17.
 */
public abstract class DecoderQueueImpl implements DecoderQueue {

    public static DecoderQueueImpl newGPUInstance(PythonDecoder.Builder builder, int[] gpus) throws DecoderException {
        return new GPUDecoderQueueImpl(builder, gpus).init();
    }

    public static DecoderQueueImpl newCPUInstance(PythonDecoder.Builder builder, int cpus) throws DecoderException {
        return new CPUDecoderQueueImpl(builder, cpus).init();
    }

    protected final Logger logger = LogManager.getLogger(getClass());

    private final int capacity;
    private final PythonDecoder.Builder processBuilder;
    private final BlockingQueue<PythonDecoder> queue;
    private final ExecutorService initExecutor;

    private final AtomicInteger aliveProcesses = new AtomicInteger(0);
    private boolean active = true;
    private DecoderListener listener;

    protected DecoderQueueImpl(PythonDecoder.Builder processBuilder, int capacity) {
        this.capacity = capacity;
        this.processBuilder = processBuilder;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.initExecutor = capacity > 1 ? Executors.newCachedThreadPool() : Executors.newSingleThreadExecutor();
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

    @Override
    public int availability() {
        return aliveProcesses.get();
    }

    @Override
    public void setListener(DecoderListener listener) {
        this.listener = listener;
    }

    @Override
    public final PythonDecoder take(LanguagePair language) throws DecoderUnavailableException {
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

    @Override
    public final PythonDecoder poll(LanguagePair language, long timeout, TimeUnit unit) throws DecoderUnavailableException {
        if (this.active && this.aliveProcesses.get() > 0) {
            try {
                return this.queue.poll(timeout, unit);
            } catch (InterruptedException e) {
                throw new DecoderUnavailableException("No NMT processes available", e);
            }
        } else {
            throw new DecoderUnavailableException("No alive NMT processes available");
        }
    }

    @Override
    public final void release(PythonDecoder process) {
        if (!this.active) {
            IOUtils.closeQuietly(process);
        } else {
            if (process.isAlive()) {
                this.queue.offer(process);
            } else {
                int availability = this.aliveProcesses.decrementAndGet();

                DecoderListener listener = this.listener;
                if (listener != null)
                    listener.onDecoderAvailabilityChanged(availability);

                IOUtils.closeQuietly(process);

                this.onProcessDied(process);

                if (this.active)
                    this.initExecutor.execute(new Initializer());
            }
        }
    }

    protected abstract PythonDecoder startProcess(PythonDecoder.Builder processBuilder) throws IOException;

    protected abstract void onProcessDied(PythonDecoder process);

    @Override
    public void close() {
        this.active = false;

        this.initExecutor.shutdownNow();
        try {
            this.initExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignore it
        }

        PythonDecoder process;
        while ((process = this.queue.poll()) != null) {
            IOUtils.closeQuietly(process);
        }
    }

    private class Initializer implements Runnable {

        @Override
        public void run() {
            PythonDecoder process;

            try {
                logger.info("Starting native decoder process");
                process = startProcess(processBuilder);
            } catch (IOException e) {
                logger.error("Failed to start new decoder process", e);
                System.exit(2);

                return;
            }

            queue.offer(process);
            int availability = aliveProcesses.incrementAndGet();

            DecoderListener listener = DecoderQueueImpl.this.listener;
            if (listener != null)
                listener.onDecoderAvailabilityChanged(availability);
        }

    }
}
