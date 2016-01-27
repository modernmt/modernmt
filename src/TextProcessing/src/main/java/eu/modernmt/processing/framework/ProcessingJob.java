package eu.modernmt.processing.framework;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Created by davide on 26/01/16.
 */
public class ProcessingJob<P, R> {

    private static final Object POISON_PILL = new Object();

    private ProcessingPipeline<P, R> pipeline;
    private PipelineInputStream<P> input;
    private PipelineOutputStream<R> output;

    private BlockingQueue<Object> inputQueue;
    private BlockingQueue<Object> outputQueue;

    private final Collector collector = new Collector();
    private final Submitter submitter = new Submitter();
    private final Outputter outputter = new Outputter();

    private Throwable error = null;

    ProcessingJob(ProcessingPipeline<P, R> pipeline, PipelineInputStream<P> input, PipelineOutputStream<R> output) {
        this.pipeline = pipeline;
        this.input = input;
        this.output = output;

        int size = Math.max(50, pipeline.getThreads() * 2);
        this.inputQueue = new ArrayBlockingQueue<>(size);
        this.outputQueue = new LinkedBlockingQueue<>();
    }

    public void start() {
        this.collector.start();
        this.submitter.start();
        this.outputter.start();
    }

    public void stop() {
        inputQueue.clear();

        try {
            // The POISON_PILL could be added twice, but this is not a problem.
            // On the contrary the clear() call could have removed the POISON_PILL
            // added by the Collector thread.
            inputQueue.put(POISON_PILL);
        } catch (InterruptedException e) {
            // Ignore
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

            while ((param = next()) != null) {
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
                Future<R> future = pipeline.submit(param);
                outputQueue.offer(future);
            }

            outputQueue.offer(POISON_PILL);
        }

    }

    private class Outputter extends Thread {

        @SuppressWarnings("unchecked")
        private R next() throws InterruptedException {
            try {
                Object result = outputQueue.take();
                if (result == POISON_PILL)
                    throw new InterruptedException();

                return ((Future<R>) result).get();
            } catch (ExecutionException e) {
                signalException(e);
                throw new InterruptedException();
            }
        }

        @Override
        public void run() {
            R result;

            try {
                while ((result = next()) != null) {
                    output.write(result);
                }
            } catch (InterruptedException e) {
                // break
            } catch (IOException e) {
                signalException(new ProcessingException("Unable to write from PipelineOutputStream", e));
            }
        }

    }

}
