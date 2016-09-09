package eu.modernmt.training.partitioning;

import eu.modernmt.model.corpus.BilingualCorpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

/**
 * Created by davide on 22/08/16.
 */
public class PartitioningUtils {

    private static final double MAX_CORPUS_PARTITION_RATIO = 0.01;

    public static double getAdjustedWeight(BilingualCorpus corpus, long extraPartitionsLines, long corporaLines) {
        int corpusLines;
        try {
            corpusLines = corpus.getLineCount();
        } catch (IOException e) {
            throw new Error("This cannot happen", e);
        }

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

    public static long countTotalCorporaLines(Collection<BilingualCorpus> corpora, int threads) throws IOException {
        ExecutorService executor = null;

        try {
            executor = threads > 1 ? Executors.newFixedThreadPool(threads) : Executors.newSingleThreadExecutor();

            ArrayList<Future<Long>> counts = new ArrayList<>(corpora.size());
            for (BilingualCorpus corpus : corpora) {
                counts.add(executor.submit(() -> (long) corpus.getLineCount()));
            }

            long count = 0;
            for (Future<Long> c : counts) {
                try {
                    count += c.get();
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

            return count;
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
