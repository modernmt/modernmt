package eu.modernmt.tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

/**
 * Created by davide on 12/11/15.
 */
public class TokenizerPool {

    public static final int DEFAULT_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final ConcurrentHashMap<Locale, TokenizerPool> cache = new ConcurrentHashMap<>();

    public static TokenizerPool getCachedInstance(Locale locale) {
        return getCachedInstance(locale, DEFAULT_POOL_SIZE);
    }

    public static TokenizerPool getCachedInstance(Locale locale, final int size) {
        return cache.computeIfAbsent(locale, (l) -> new TokenizerPool(Tokenizers.getFactory(l), size));
    }

    private ExecutorService executor;
    private ITokenizerFactory tokenizerFactory;
    private ArrayBlockingQueue<ITokenizer> tokenizerInstances;
    private int size;
    private int availableTokenizers;

    public TokenizerPool(ITokenizerFactory tokenizerFactory) {
        this(tokenizerFactory, DEFAULT_POOL_SIZE);
    }

    public TokenizerPool(ITokenizerFactory tokenizerFactory, int size) {
        this.tokenizerFactory = tokenizerFactory;
        this.size = size;
        this.availableTokenizers = 0;

        this.executor = Executors.newFixedThreadPool(size);
        this.tokenizerInstances = new ArrayBlockingQueue<>(size);
    }

    public List<String[]> tokenize(List<String> strings) {
        return this.tokenize(strings.toArray(new String[strings.size()]));
    }

    public List<String[]> tokenize(final String[] strings) {
        int size = (int) Math.ceil(((double) strings.length) / this.size);
        int parts = (int) Math.ceil(((double) strings.length) / size);

        ArrayList<Future<List<String[]>>> results = new ArrayList<>(parts);
        for (int i = 0; i < strings.length; i += size) {
            TokenizeTask task = new TokenizeTask(strings, i, size);
            Future<List<String[]>> result = this.executor.submit(task);
            results.add(result);
        }

        ArrayList<String[]> tokenizedStrings = new ArrayList<>(strings.length);

        for (Future<List<String[]>> result : results) {
            List<String[]> tokens;

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

            tokenizedStrings.addAll(tokens);
        }

        return tokenizedStrings;
    }

    protected ITokenizer getTokenizerInstance() {
        ITokenizer tokenizer = this.tokenizerInstances.poll();

        if (this.availableTokenizers < size) {
            boolean createNewInstance = false;

            synchronized (this) {
                if (this.availableTokenizers < size) {
                    this.availableTokenizers++;
                    createNewInstance = true;
                }
            }

            if (createNewInstance)
                tokenizer = this.tokenizerFactory.create();
        }

        if (tokenizer == null)
            try {
                tokenizer = this.tokenizerInstances.take();
            } catch (InterruptedException e) {
                return null;
            }

        return tokenizer;
    }

    protected void releaseTokenizerInstance(ITokenizer tokenizer) {
        if (tokenizer != null)
            this.tokenizerInstances.add(tokenizer);
    }

    public void terminate() {
        this.executor.shutdownNow();
    }

    private class TokenizeTask implements Callable<List<String[]>> {

        private String[] strings;
        private int offset;
        private int size;

        public TokenizeTask(String[] strings, int offset, int size) {
            this.strings = strings;
            this.offset = offset;
            this.size = Math.min(size, strings.length - offset);
        }

        @Override
        public List<String[]> call() {
            ITokenizer tokenizer = null;

            try {
                tokenizer = TokenizerPool.this.getTokenizerInstance();
                ArrayList<String[]> tokenizedStrings = new ArrayList<>(this.size);

                for (int i = 0; i < this.size; i++)
                    tokenizedStrings.add(tokenizer.tokenize(this.strings[this.offset + i]));

                return tokenizedStrings;
            } finally {
                TokenizerPool.this.releaseTokenizerInstance(tokenizer);
            }
        }
    }

}
