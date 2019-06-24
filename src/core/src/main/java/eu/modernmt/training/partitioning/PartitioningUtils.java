package eu.modernmt.training.partitioning;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.util.*;

/**
 * Created by davide on 22/08/16.
 */
public class PartitioningUtils {

    private static final double MAX_CORPUS_PARTITION_RATIO = 0.01;

    public static double getAdjustedWeight(LanguageDirection language, MultilingualCorpus corpus, long extraPartitionsLines, long corporaLines) {
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

}
