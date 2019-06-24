package eu.modernmt.decoder.neural.execution.impl;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderListener;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.decoder.neural.ModelConfig;
import eu.modernmt.decoder.neural.execution.DecoderQueue;
import eu.modernmt.decoder.neural.execution.PythonDecoder;
import eu.modernmt.lang.LanguageDirection;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by davide on 22/05/17.
 */
public class DecoderQueueImpl implements DecoderQueue {

    public static DecoderQueueImpl newGPUInstance(ModelConfig config, PythonDecoder.Builder builder, int[] gpus) throws DecoderException {
        Map<LanguageDirection, File> checkpoints = config.getAvailableModels();

        Handler[] handlers = new Handler[gpus.length];
        for (int i = 0; i < gpus.length; i++)
            handlers[i] = new Handler(builder, checkpoints, gpus[i]);

        return new DecoderQueueImpl(checkpoints, handlers);
    }

    public static DecoderQueueImpl newCPUInstance(ModelConfig config, PythonDecoder.Builder builder, int cpus) throws DecoderException {
        Map<LanguageDirection, File> checkpoints = config.getAvailableModels();

        Handler[] handlers = new Handler[cpus];
        for (int i = 0; i < cpus; i++)
            handlers[i] = new Handler(builder, checkpoints, -1);

        return new DecoderQueueImpl(checkpoints, handlers);
    }

    protected final Logger logger = LogManager.getLogger(getClass());

    private final Map<LanguageDirection, File> checkpoints;
    private final BlockingQueue<Handler> queue;
    private final ExecutorService initExecutor;
    private final int maxAvailability;

    private final AtomicInteger aliveProcesses = new AtomicInteger(0);
    private boolean active = true;
    private DecoderListener listener;

    protected DecoderQueueImpl(Map<LanguageDirection, File> checkpoints, Handler[] handlers) throws DecoderException {
        this.checkpoints = checkpoints;
        this.queue = new ArrayBlockingQueue<>(handlers.length);
        this.maxAvailability = handlers.length;
        this.initExecutor = handlers.length > 1 ? Executors.newCachedThreadPool() : Executors.newSingleThreadExecutor();

        Future<?>[] array = new Future<?>[handlers.length];
        for (int i = 0; i < array.length; i++)
            array[i] = this.initExecutor.submit(new Initializer(handlers[i]));

        for (Future<?> future : array) {
            try {
                future.get();
            } catch (InterruptedException e) {
                throw new DecoderException("Initialization interrupted", e);
            } catch (ExecutionException e) {
                throw new DecoderException("Unexpected error during initialization", e.getCause());
            }
        }
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
    public final PythonDecoder take(LanguageDirection language) throws DecoderUnavailableException {
        return this.poll(language, 0L, null);
    }

    @Override
    public final PythonDecoder poll(LanguageDirection language, long timeout, TimeUnit unit) throws DecoderUnavailableException {
        if (!this.active || this.aliveProcesses.get() == 0)
            throw new DecoderUnavailableException("No alive NMT processes available");

        PythonDecoder decoder = null;
        try {
            if (language != null)
                decoder = tryGetByLanguagePair(language);

            if (decoder == null) {
                if (timeout > 0)
                    decoder = this.queue.poll(timeout, unit);
                else
                    decoder = this.queue.take();
            }

            return decoder;
        } catch (InterruptedException e) {
            throw new DecoderUnavailableException("No NMT processes available", e);
        } finally {
            if (decoder != null)
                ((Handler) decoder).setInUse();
        }
    }

    private PythonDecoder tryGetByLanguagePair(LanguageDirection language) {
        File checkpoint = checkpoints.get(language);

        Iterator<Handler> iterator = this.queue.iterator();
        while (iterator.hasNext()) {
            Handler handler = iterator.next();

            if (checkpoint.equals(handler.getLastCheckpoint())) {
                iterator.remove();
                return handler;
            }
        }

        return null;
    }

    @Override
    public final void release(PythonDecoder process) {
        Handler handler = (Handler) process;

        if (!handler.unsetInUse()) {
            logger.warn("Attempt to call release() twice on GPU " + handler.getGPU() + " process");
            return;
        }

        if (!this.active) {
            IOUtils.closeQuietly(handler);
        } else {
            if (handler.isAlive()) {
                this.queue.offer(handler);
            } else {
                int availability = this.aliveProcesses.decrementAndGet();

                DecoderListener listener = this.listener;
                if (listener != null)
                    listener.onDecoderAvailabilityChanged(availability, this.maxAvailability);

                if (this.active)
                    this.initExecutor.execute(new Initializer(handler));
            }
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

        PythonDecoder process;
        while ((process = this.queue.poll()) != null) {
            IOUtils.closeQuietly(process);
        }
    }

    private class Initializer implements Runnable {

        private final Handler handler;

        private Initializer(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            int gpu = handler.getGPU();
            String msg = gpu < 0 ? "Native decoder process on CPU" : ("Native decoder process on GPU " + gpu);

            try {
                logger.info(msg + " is starting");

                long begin = System.currentTimeMillis();
                handler.restart();
                long elapsed = System.currentTimeMillis() - begin;

                logger.info(msg + " started in " + (elapsed / 1000) + "s");
            } catch (IOException e) {
                logger.error(msg + " failed to start", e);
                System.exit(2);
            }

            queue.offer(handler);
            int availability = aliveProcesses.incrementAndGet();

            DecoderListener listener = DecoderQueueImpl.this.listener;
            if (listener != null)
                listener.onDecoderAvailabilityChanged(availability, DecoderQueueImpl.this.maxAvailability);
        }

    }

}
