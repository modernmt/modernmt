package eu.modernmt.training.preprocessing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.ProcessingPipeline;

import java.io.Closeable;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.*;

/**
 * Created by davide on 19/08/16.
 */
public class TrainingPreprocessor implements Closeable {

    private final int threads;
    private final Locale language;

    private final ExecutorService executor;

    public TrainingPreprocessor(int threads, Locale language) {
        this.threads = threads;
        this.language = language;

        this.executor = Executors.newFixedThreadPool(threads);
    }

    public String[][] process(String[] batch) throws ProcessingException {
        String[][] output = new String[batch.length][];
        Future<?>[] locks = new Future<?>[threads];

        if (batch.length < threads) {
            locks[0] = executor.submit(new FragmentProcessor(batch, output, 0, batch.length));
        } else {
            int fragmentSize = batch.length / threads;

            for (int i = 0; i < threads; i++) {
                int offset = i * fragmentSize;
                int length = fragmentSize;

                if (i == threads - 1)
                    length = batch.length - offset;

                locks[i] = executor.submit(new FragmentProcessor(batch, output, offset, length));
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

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS))
                executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private class FragmentProcessor implements Callable<Void> {

        private final String[] batch;
        private final String[][] output;
        private final int offset;
        private final int length;

        public FragmentProcessor(String[] batch, String[][] output, int offset, int length) {
            this.batch = batch;
            this.output = output;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public Void call() throws ProcessingException {
            ProcessingPipeline<String, Sentence> pipeline;

            try {
                pipeline = Preprocessor.createPipeline(language);
            } catch (IOException e) {
                throw new ProcessingException("Unable to load pipeline", e);
            }

            for (int i = 0; i < length; i++) {
                Word[] words = pipeline.call(batch[offset + i]).getWords();
                batch[offset + i] = null; // free memory

                String[] array = new String[words.length];
                for (int j = 0; j < array.length; j++)
                    array[j] = words[j].getPlaceholder();

                output[offset + i] = array;
            }

            return null;
        }
    }

}
