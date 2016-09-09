package eu.modernmt.processing.concurrent;

import eu.modernmt.processing.PipelineInputStream;
import eu.modernmt.processing.PipelineOutputStream;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.ProcessingPipeline;
import eu.modernmt.processing.builder.PipelineBuilder;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by davide on 31/05/16.
 */
public class PipelineExecutor<P, R> {

    private final ExecutorService executor;
    private final Queue<ProcessingPipeline<P, R>> pipelineBuffer;

    private final Locale source;
    private final Locale target;
    private final PipelineBuilder<P, R> builder;
    private final int threads;

    public PipelineExecutor(Locale source, Locale target, PipelineBuilder<P, R> builder, int threads) {
        this.executor = Executors.newFixedThreadPool(threads);
        this.pipelineBuffer = new ConcurrentLinkedQueue<>();

        this.source = source;
        this.target = target;
        this.builder = builder;
        this.threads = threads;
    }

    public void shutdownNow() {
        executor.shutdownNow();
    }

    public void shutdown() {
        executor.shutdown();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    public int getThreads() {
        return threads;
    }

    public R process(P value) throws ProcessingException {
        return process(value, null);
    }

    public R process(P value, Map<String, Object> metadata) throws ProcessingException {
        try {
            return submit(value, metadata).get();
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

    public List<R> process(Collection<P> input) throws ProcessingException {
        return process(input, null);
    }

    public List<R> process(Collection<P> input, Map<String, Object> metadata) throws ProcessingException {
        BatchTask<P, R> task = new BatchTask<>(input);
        process(task, task, metadata);
        return task.getOutput();
    }

    public void process(PipelineInputStream<P> input, PipelineOutputStream<R> output) throws ProcessingException {
        process(input, output, null);
    }

    public void process(PipelineInputStream<P> input, PipelineOutputStream<R> output, Map<String, Object> metadata) throws ProcessingException {
        ProcessingJob<P, R> job = new ProcessingJob<>(this, input, output);

        if (metadata != null)
            job.setMetadata(metadata);

        job.start();

        try {
            job.join();
        } catch (InterruptedException e) {
            // Ignore it
        }
    }

    Future<R> submit(P param, Map<String, Object> metadata) {
        return this.executor.submit(new Task(param, metadata));
    }

    private ProcessingPipeline<P, R> getPipeline() throws ProcessingException {
        ProcessingPipeline<P, R> instance = pipelineBuffer.poll();

        if (instance == null)
            instance = builder.newPipeline(source, target);

        return instance;
    }

    private void releasePipeline(ProcessingPipeline<P, R> pipeline) {
        pipelineBuffer.add(pipeline);
    }

    private class Task implements Callable<R> {

        private final P param;
        private final Map<String, Object> metadata;

        private Task(P param, Map<String, Object> metadata) {
            this.param = param;
            this.metadata = metadata;
        }

        @Override
        @SuppressWarnings("unchecked")
        public R call() throws ProcessingException {
            Map<String, Object> metadata = (this.metadata == null) ? new HashMap<>() : new HashMap<>(this.metadata);
            ProcessingPipeline<P, R> pipeline = getPipeline();

            try {
                return pipeline.call(param, metadata);
            } finally {
                releasePipeline(pipeline);
            }
        }
    }
}
