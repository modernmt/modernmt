package eu.modernmt.training;

import eu.modernmt.io.Corpora;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.MaskedMultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.training.partitioning.CorporaPartition;
import eu.modernmt.training.partitioning.PartitioningUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by davide on 22/08/16.
 */
public class PreprocessingPipeline {

    private final int threads;
    private final CorporaPartition mainPartition;
    private final LanguageDirection language;

    private ArrayList<CorporaPartition> extraPartitions = new ArrayList<>();

    public PreprocessingPipeline(LanguageDirection language, CorporaPartition mainPartition) {
        this(language, mainPartition, Runtime.getRuntime().availableProcessors() * 2);
    }

    public PreprocessingPipeline(LanguageDirection language, CorporaPartition mainPartition, int threads) {
        this.threads = threads;
        this.mainPartition = mainPartition;
        this.language = language;
    }

    public void addExtraPartition(CorporaPartition partition) {
        this.extraPartitions.add(partition);
    }

    public void process(Collection<MultilingualCorpus> corpora) throws ProcessingException, IOException {
        // Masking input corpora
        ArrayList<MultilingualCorpus> maskedMultilingualCorpora = new ArrayList<>(corpora.size());
        for (MultilingualCorpus corpus : corpora)
            maskedMultilingualCorpora.add(new MaskedMultilingualCorpus(language, corpus));

        // Start processing
        Preprocessor preprocessor = new Preprocessor(threads);

        try {
            Map<LanguageDirection, Long> bilingualCorporaLinesMap = Corpora.countLines(maskedMultilingualCorpora, threads);
            long extraPartitionsLines = PartitioningUtils.countTotalPartitionsLines(extraPartitions);

            for (MultilingualCorpus corpus : maskedMultilingualCorpora) {
                for (LanguageDirection language : corpus.getLanguages()) {
                    long bilingualCorporaLines = bilingualCorporaLinesMap.get(language);
                    double weight = PartitioningUtils.getAdjustedWeight(language, corpus, extraPartitionsLines, bilingualCorporaLines);
                    int lineCount = corpus.getLineCount(language);

                    PreprocessingTask sourceTask = new PreprocessingTask(preprocessor, language, corpus.getCorpus(language, true), lineCount, mainPartition);
                    PreprocessingTask targetTask = new PreprocessingTask(preprocessor, language.reversed(), corpus.getCorpus(language, false), lineCount, mainPartition);

                    for (CorporaPartition partition : extraPartitions) {
                        int size = (int) Math.round(weight * partition.getSize());
                        if (size > 0) {
                            sourceTask.addExtraPartition(partition, size);
                            targetTask.addExtraPartition(partition, size);
                        }
                    }

                    sourceTask.execute();
                    targetTask.execute();
                }
            }
        } finally {
            IOUtils.closeQuietly(preprocessor);
        }
    }

}
