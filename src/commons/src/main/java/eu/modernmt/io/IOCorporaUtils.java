package eu.modernmt.io;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by davide on 04/05/17.
 */
public class IOCorporaUtils {

    private static final int MAX_IO_THREADS = 10;

    // Word count ------------------------------------------------------------------------------------------------------

    private static class Counter {

        public long value = 0;

    }

    public static Map<LanguagePair, Long> wordCount(MultilingualCorpus corpus) throws IOException {
        Map<LanguagePair, Counter> counts = doWordCount(corpus);
        return getCounts(counts);
    }

    public static Map<LanguagePair, Long> wordCount(Collection<? extends MultilingualCorpus> corpora) throws IOException {
        return wordCount(corpora, 1);
    }

    public static Map<LanguagePair, Long> wordCount(Collection<? extends MultilingualCorpus> corpora, int threads) throws IOException {
        threads = Math.min(threads, MAX_IO_THREADS);

        ExecutorService executor = threads > 1 ? Executors.newFixedThreadPool(threads) : Executors.newSingleThreadExecutor();
        ExecutorCompletionService<Map<LanguagePair, Counter>> results = new ExecutorCompletionService<>(executor);

        for (MultilingualCorpus corpus : corpora)
            results.submit(() -> doWordCount(corpus));

        Map<LanguagePair, Counter> accumulator = null;

        try {

            for (int i = 0; i < corpora.size(); i++) {
                try {
                    Map<LanguagePair, Counter> result = results.take().get();

                    if (accumulator == null) {
                        accumulator = result;
                    } else {
                        for (Map.Entry<LanguagePair, Counter> e : result.entrySet())
                            accumulator.computeIfAbsent(e.getKey(), key -> new Counter()).value += e.getValue().value;
                    }
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();

                    if (cause instanceof IOException) {
                        throw (IOException) cause;
                    } else if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    } else {
                        throw new Error("Unexpected exception", cause);
                    }
                } catch (InterruptedException e) {
                    throw new IOException("Execution interrupted", e);
                }
            }
        } finally {
            executor.shutdownNow();
        }

        return getCounts(accumulator);
    }

    private static Map<LanguagePair, Counter> doWordCount(MultilingualCorpus corpus) throws IOException {
        HashMap<LanguagePair, Counter> counts = new HashMap<>();

        MultilingualCorpus.MultilingualLineReader reader = null;

        try {
            reader = corpus.getContentReader();

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                Counter counter = counts.computeIfAbsent(pair.language, key -> new Counter());
                counter.value += WordCounter.count(pair.source, pair.language.source);
            }

            return counts;
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private static Map<LanguagePair, Long> getCounts(Map<LanguagePair, Counter> counts) {
        HashMap<LanguagePair, Long> result = new HashMap<>(counts.size());
        for (Map.Entry<LanguagePair, Counter> entry : counts.entrySet())
            result.put(entry.getKey(), entry.getValue().value);
        return result;
    }

    // Copy ------------------------------------------------------------------------------------------------------------

    public static void copy(Corpus source, Corpus destination, long linesLimit, boolean append) throws IOException {
        LineReader reader = null;
        LineWriter writer = null;

        try {
            reader = source.getContentReader();
            writer = destination.getContentWriter(append);
            String line;
            long lines = 0;
            while ((line = reader.readLine()) != null && lines++ < linesLimit)
                writer.writeLine(line);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

    public static void copy(Corpus source, Corpus destination, long linesLimit) throws IOException {
        copy(source, destination, linesLimit, false);
    }

    public static void copy(Corpus source, Corpus destination, boolean append) throws IOException {
        copy(source, destination, Long.MAX_VALUE, append);
    }

    public static void copy(Corpus source, Corpus destination) throws IOException {
        copy(source, destination, Long.MAX_VALUE);
    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination, long pairsLimit, boolean append) throws IOException {
        MultilingualCorpus.MultilingualLineReader reader = null;
        MultilingualCorpus.MultilingualLineWriter writer = null;

        try {
            reader = source.getContentReader();
            writer = destination.getContentWriter(append);

            MultilingualCorpus.StringPair pair;
            long pairs = 0;
            while ((pair = reader.read()) != null && pairs++ < pairsLimit)
                writer.write(pair);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination, long linesLimit) throws IOException {
        copy(source, destination, linesLimit, false);
    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination, boolean append) throws IOException {
        copy(source, destination, Long.MAX_VALUE, append);
    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination) throws IOException {
        copy(source, destination, Long.MAX_VALUE);
    }

}
