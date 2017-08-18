package eu.modernmt.training.partitioning;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by davide on 22/08/16.
 */
public class PartitioningUtils {

    private static final double MAX_CORPUS_PARTITION_RATIO = 0.01;

    public static double getAdjustedWeight(LanguagePair language, MultilingualCorpus corpus, long extraPartitionsLines, long corporaLines) {
        int corpusLines = corpus.getLineCount(language);

        double weight = ((double) corpusLines) / corporaLines;
        int expectedSize = (int) Math.round(weight * extraPartitionsLines);
        int maxAllowedLines = (int) Math.round(corpusLines * MAX_CORPUS_PARTITION_RATIO);

        if (expectedSize > maxAllowedLines) {
            return ((double) maxAllowedLines) / extraPartitionsLines;
        } else {
            return weight;
        }
    }

    public static long countTotalPartitionsLines(Collection<CorporaPartition> partitions) {
        long count = 0;

        for (CorporaPartition partition : partitions)
            count += partition.getSize();

        return count;
    }

    public static Map<LanguagePair, Long> countTotalCorporaLines(Collection<MultilingualCorpus> corpora, int threads) throws IOException {
        ExecutorService executor = null;

        try {
            executor = threads > 1 ? Executors.newFixedThreadPool(threads) : Executors.newSingleThreadExecutor();

            ArrayList<Future<HashMap<LanguagePair, Long>>> futures = new ArrayList<>(corpora.size());

            for (MultilingualCorpus corpus : corpora) {
                futures.add(executor.submit(() -> {
                    Set<LanguagePair> languages = corpus.getLanguages();
                    HashMap<LanguagePair, Long> counts = new HashMap<>(languages.size());

                    for (LanguagePair language : languages)
                        counts.put(language, (long) corpus.getLineCount(language));

                    return counts;
                }));
            }

            HashMap<LanguagePair, Long> result = new HashMap<>();

            for (Future<HashMap<LanguagePair, Long>> future : futures) {
                try {
                    for (Map.Entry<LanguagePair, Long> count : future.get().entrySet()) {
                        Long old = result.get(count.getKey());
                        result.put(count.getKey(), (old == null ? 0L : old) + count.getValue());
                    }
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();

                    if (cause instanceof IOException)
                        throw (IOException) cause;
                    else if (cause instanceof RuntimeException)
                        throw (RuntimeException) cause;
                    else
                        throw new Error("Unexpected exception", cause);
                } catch (InterruptedException e) {
                    throw new IOException("Interrupted execution", e);
                }
            }

            return result;
        } finally {
            if (executor != null) {
                executor.shutdown();

                try {
                    if (!executor.awaitTermination(1, TimeUnit.SECONDS))
                        executor.shutdownNow();
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                }
            }
        }
    }

}
