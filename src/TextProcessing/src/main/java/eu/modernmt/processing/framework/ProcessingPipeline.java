package eu.modernmt.processing.framework;

import java.io.Closeable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by davide on 26/01/16.
 */
public class ProcessingPipeline<P, R> implements Closeable {

    public static class Builder<P, R> {

        private int threads = Runtime.getRuntime().availableProcessors();
        private LinkedList<TextProcessor<?, ?>> processors = new LinkedList<>();

        public Builder<P, R> setThreads(int threads) {
            this.threads = threads;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <Q> Builder<P, Q> add(TextProcessor<R, Q> processor) {
            if (processor != null)
                processors.add(processor);
            return (Builder<P, Q>) this;
        }

        public ProcessingPipeline<P, R> create() {
            ProcessingPipeline<P, R> pipeline = new ProcessingPipeline<>(threads);
            processors.forEach(pipeline::add);

            return pipeline;
        }

    }

    protected int threads;
    protected ExecutorService executor;
    protected LinkedList<TextProcessor<Object, Object>> processors = new LinkedList<>();

    protected ProcessingPipeline(int threads) {
        this.threads = threads;
        this.executor = Executors.newFixedThreadPool(threads);
    }

    public int getThreads() {
        return threads;
    }

    @SuppressWarnings("unchecked")
    private void add(TextProcessor<?, ?> processor) {
        this.processors.add((TextProcessor<Object, Object>) processor);
    }

    public R process(P value) throws ProcessingException {
        try {
            return submit(value).get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else if (cause instanceof ProcessingException)
                throw (ProcessingException) cause;
            else
                throw new RuntimeException("Unexpected exception", cause);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public Future<R> submit(P value) {
        return executor.submit(new Task(value));
    }

    public ProcessingJob<P, R> createJob(PipelineInputStream<P> input, PipelineOutputStream<R> output) {
        return new ProcessingJob<>(this, input, output);
    }

    @Override
    public void close() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    private class Task implements Callable<R> {

        private P param;

        public Task(P param) {
            this.param = param;
        }

        @Override
        @SuppressWarnings("unchecked")
        public R call() throws ProcessingException {
            Map<String, Object> metadata = new HashMap<>();
            Object result = param;

            for (TextProcessor<Object, Object> processor : processors) {
                result = processor.call(result, metadata);
            }

            return (R) result;
        }
    }

}
