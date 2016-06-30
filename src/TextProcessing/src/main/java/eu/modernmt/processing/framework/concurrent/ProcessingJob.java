package eu.modernmt.processing.framework.concurrent;

import eu.modernmt.processing.framework.PipelineInputStream;
import eu.modernmt.processing.framework.PipelineOutputStream;
import eu.modernmt.processing.framework.ProcessingException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by davide on 31/05/16.
 */
class ProcessingJob<P, R> {

    private static final Object POISON_PILL = new Object();

    private PipelineExecutor<P, R> executor;
    private Map<String, Object> metadata = null;
    private PipelineInputStream<P> input;
    private PipelineOutputStream<R> output;

    private BlockingQueue<Object> inputQueue;
    private BlockingQueue<Object> outputQueue;

    private final Collector collector = new Collector();
    private final Submitter submitter = new Submitter();
    private final Outputter outputter = new Outputter();

    private Throwable error = null;

    ProcessingJob(PipelineExecutor<P, R> executor, PipelineInputStream<P> input, PipelineOutputStream<R> output) {
        this.executor = executor;
        this.input = input;
        this.output = output;

        int size = Math.max(50, executor.getThreads() * 2);
        this.inputQueue = new ArrayBlockingQueue<>(size);
        this.outputQueue = new ArrayBlockingQueue<>(size);
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void start() {
        this.collector.start();
        this.submitter.start();
        this.outputter.start();
    }

    public void stop() {
        collector.terminate();

        try {
            // The POISON_PILL could be added twice, but this is not a problem.
            // On the contrary the clear() call could have removed the POISON_PILL
            // added by the Collector thread.
            inputQueue.clear();
            inputQueue.put(POISON_PILL);
        } catch (InterruptedException e) {
            // Ignore
        } finally {
            outputQueue.clear();
        }
    }

    private void signalException(Throwable exception) {
        this.error = exception;
        this.stop();
    }

    public void join() throws InterruptedException, ProcessingException {
        this.collector.join();
        this.submitter.join();
        this.outputter.join();

        if (error != null) {
            if (error instanceof ExecutionException)
                error = error.getCause();

            if (error instanceof InterruptedException)
                throw (InterruptedException) error;
            else if (error instanceof ProcessingException)
                throw (ProcessingException) error;
            else if (error instanceof RuntimeException)
                throw (RuntimeException) error;
            else
                throw new Error("Unexpected exception", error);
        }
    }

    private class Collector extends Thread {

        private boolean terminated = false;

        public void terminate() {
            terminated = true;
        }

        private P next() {
            try {
                return input.read();
            } catch (IOException e) {
                signalException(new ProcessingException("Unable to read from PipelineInputStream", e));
                return null;
            }
        }

        @Override
        public void run() {
            P param;

            while (!terminated && (param = next()) != null) {
                try {
                    inputQueue.put(param);
                } catch (InterruptedException e) {
                    signalException(e);
                    break;
                }
            }

            try {
                inputQueue.put(POISON_PILL);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

    }

    private class Submitter extends Thread {

        @SuppressWarnings("unchecked")
        private P next() {
            try {
                Object result = inputQueue.take();
                return result == POISON_PILL ? null : (P) result;
            } catch (InterruptedException e) {
                return null;
            }
        }

        @Override
        public void run() {
            P param;

            while ((param = next()) != null) {
                try {
                    Future<R> future = executor.submit(param, metadata);
                    outputQueue.put(future);
                } catch (RejectedExecutionException | InterruptedException e) {
                    signalException(e);
                    break;
                }
            }

            try {
                outputQueue.put(POISON_PILL);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

    }

    private class Outputter extends Thread {

        @SuppressWarnings("unchecked")
        private Future<R> next() throws InterruptedException {
            Object result = outputQueue.take();
            if (result == POISON_PILL)
                return null;

            return ((Future<R>) result);
        }

        @Override
        public void run() {
            Future<R> result;

            try {
                while ((result = next()) != null) {
                    R value = result.get();
                    if (output != null)
                        output.write(value);
                }
            } catch (InterruptedException e) {
                // break
            } catch (ExecutionException e) {
                signalException(e);
                // break
            } catch (IOException e) {
                signalException(new ProcessingException("Unable to write to PipelineOutputStream", e));
            }
        }

    }

}
