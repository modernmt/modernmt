package eu.modernmt.tokenizer;

import eu.modernmt.tokenizer.moses.MosesDetokenizerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

/**
 * Created by davide on 12/11/15.
 */
public class DetokenizerPool {

    // TODO: it should work with Tokenizers and Detokenizers

    public static final int DEFAULT_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final ConcurrentHashMap<Locale, DetokenizerPool> cache = new ConcurrentHashMap<>();

    public static DetokenizerPool getCachedInstance(Locale locale) {
        return getCachedInstance(locale, DEFAULT_POOL_SIZE);
    }

    public static DetokenizerPool getCachedInstance(Locale locale, final int size) {
        return cache.computeIfAbsent(locale, (l) -> new DetokenizerPool(new MosesDetokenizerFactory(l.toLanguageTag()), size));
    }

    private ExecutorService executor;
    private IDetokenizerFactory detokenizerFactory;
    private ArrayBlockingQueue<IDetokenizer> detokenizerInstances;
    private int size;
    private int availableDetokenizers;

    public DetokenizerPool(IDetokenizerFactory detokenizerFactory) {
        this(detokenizerFactory, DEFAULT_POOL_SIZE);
    }

    public DetokenizerPool(IDetokenizerFactory detokenizerFactory, int size) {
        this.detokenizerFactory = detokenizerFactory;
        this.size = size;
        this.availableDetokenizers = 0;

        this.executor = Executors.newFixedThreadPool(size);
        this.detokenizerInstances = new ArrayBlockingQueue<>(size);
    }

    public String detokenize(String[] tokens) {
        return this.detokenize(Collections.singletonList(tokens)).get(0);
    }

    public List<String> detokenize(List<String[]> strings) {
        int size = (int) Math.ceil(((double) strings.size()) / this.size);
        int parts = (int) Math.ceil(((double) strings.size()) / size);

        ArrayList<Future<List<String>>> results = new ArrayList<>(parts);
        for (int i = 0; i < strings.size(); i += size) {
            DetokenizeTask task = new DetokenizeTask(strings, i, size);
            Future<List<String>> result = this.executor.submit(task);
            results.add(result);
        }

        ArrayList<String> detokenizedStrings = new ArrayList<>(strings.size());

        for (Future<List<String>> result : results) {
            List<String> tokens;

            try {
                tokens = result.get();
            } catch (InterruptedException e) {
                return null;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new RuntimeException("This cannot happen", e);
                }
            }

            detokenizedStrings.addAll(tokens);
        }

        return detokenizedStrings;
    }

    protected IDetokenizer getDetokenizerInstance() {
        IDetokenizer detokenizer = this.detokenizerInstances.poll();

        if (this.availableDetokenizers < size) {
            boolean createNewInstance = false;

            synchronized (this) {
                if (this.availableDetokenizers < size) {
                    this.availableDetokenizers++;
                    createNewInstance = true;
                }
            }

            if (createNewInstance)
                detokenizer = this.detokenizerFactory.create();
        }

        if (detokenizer == null)
            try {
                detokenizer = this.detokenizerInstances.take();
            } catch (InterruptedException e) {
                return null;
            }

        return detokenizer;
    }

    protected void releaseDetokenizerInstance(IDetokenizer detokenizer) {
        if (detokenizer != null)
            this.detokenizerInstances.add(detokenizer);
    }

    public void terminate() {
        this.executor.shutdownNow();
    }

    private class DetokenizeTask implements Callable<List<String>> {

        private List<String[]> strings;
        private int offset;
        private int size;

        public DetokenizeTask(List<String[]> strings, int offset, int size) {
            this.strings = strings;
            this.offset = offset;
            this.size = Math.min(size, strings.size() - offset);
        }

        @Override
        public List<String> call() {
            IDetokenizer detokenizer = null;

            try {
                detokenizer = DetokenizerPool.this.getDetokenizerInstance();
                ArrayList<String> detokenizedStrings = new ArrayList<>(this.size);

                for (int i = 0; i < this.size; i++)
                    detokenizedStrings.add(detokenizer.detokenize(this.strings.get(this.offset + i)));

                return detokenizedStrings;
            } finally {
                DetokenizerPool.this.releaseDetokenizerInstance(detokenizer);
            }
        }
    }

}
