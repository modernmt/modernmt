package eu.modernmt.training.preprocessing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.ProcessingPipeline;

import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by davide on 19/08/16.
 */
class PreprocessorExecutor {

    private final int threads;
    private final Locale language;

    private final ExecutorService executor;
    private final Future<?>[] locks;

    public PreprocessorExecutor(int threads, Locale language) {
        this.threads = threads;
        this.language = language;

        this.executor = Executors.newFixedThreadPool(threads);
        this.locks = new Future<?>[threads];
    }

    public String[][] start(String[] batch) {
        String[][] output = new String[batch.length][];

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

        return output;
    }

    public void await() {
        for (Future<?> lock : locks) {
            if (lock == null)
                break;

            try {
                lock.get();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        for (int i = 0; i < threads; i++)
            locks[i] = null;
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
            ProcessingPipeline<String, Sentence> pipeline = Preprocessor.createPipeline(language);

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
