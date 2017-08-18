package eu.modernmt.processing.concurrent;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.ProcessingPipeline;
import eu.modernmt.processing.builder.PipelineBuilder;

import java.util.concurrent.*;

/**
 * Created by davide on 31/05/16.
 */
public class PipelineExecutor<P, R> {

    private final PipelineQueue<P, R> pipelines;
    private final ExecutorService executor;
    private final int threads;

    public PipelineExecutor(PipelineBuilder<P, R> builder, int threads) {
        this.pipelines = new PipelineQueue<>(builder);
        this.executor = threads > 1 ? Executors.newFixedThreadPool(threads) : Executors.newSingleThreadExecutor();
        this.threads = threads;
    }

    public R process(LanguagePair language, P input) throws ProcessingException {
        return pipelines.get(language).call(input);
    }

    public R[] processBatch(LanguagePair language, P[] batch, R[] output) throws ProcessingException {
        Future<?>[] locks = new Future<?>[threads];

        if (batch.length < threads) {
            locks[0] = executor.submit(new FragmentTask(language, batch, output, 0, batch.length));
        } else {
            int fragmentSize = batch.length / threads;

            for (int i = 0; i < threads; i++) {
                int offset = i * fragmentSize;
                int length = fragmentSize;

                if (i == threads - 1)
                    length = batch.length - offset;

                locks[i] = executor.submit(new FragmentTask(language, batch, output, offset, length));
            }
        }

        for (Future<?> lock : locks) {
            if (lock == null)
                break;

            try {
                lock.get();
            } catch (InterruptedException e) {
                throw new ProcessingException("Execution interrupted", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();

                if (cause instanceof ProcessingException)
                    throw (ProcessingException) cause;
                else if (cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                else
                    throw new Error("Unexpected exception", cause);
            }
        }

        return output;
    }

    public void shutdown() {
        executor.shutdown();
    }

    public boolean awaitTermination(int timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    public void shutdownNow() {
        executor.shutdownNow();
    }

    public class FragmentTask implements Callable<Void> {

        private final LanguagePair language;
        private final P[] batch;
        private final Object[] output;
        private final int offset;
        private final int length;

        public FragmentTask(LanguagePair language, P[] batch, R[] output, int offset, int length) {
            this.language = language;
            this.batch = batch;
            this.output = output;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public Void call() throws ProcessingException {
            ProcessingPipeline<P, R> pipeline = pipelines.get(language);

            try {
                for (int i = 0; i < length; i++) {
                    output[offset + i] = pipeline.call(batch[offset + i]);
                    batch[offset + i] = null; // free memory
                }

                return null;
            } finally {
                pipelines.release(language, pipeline);
            }
        }
    }


}
