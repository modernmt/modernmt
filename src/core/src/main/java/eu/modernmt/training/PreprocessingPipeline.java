package eu.modernmt.training;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.training.partitioning.CorporaPartition;
import eu.modernmt.training.partitioning.PartitioningUtils;
import eu.modernmt.training.preprocessing.CorpusWriter;
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
    private final CorpusWriter corpusWriter;

    private ArrayList<CorporaPartition> extraPartitions = new ArrayList<>();

    public PreprocessingPipeline(CorporaPartition mainPartition, CorpusWriter writer) {
        this(mainPartition, writer, Runtime.getRuntime().availableProcessors() * 2);
    }

    public PreprocessingPipeline(CorporaPartition mainPartition, CorpusWriter writer, int threads) {
        this.threads = threads;
        this.mainPartition = mainPartition;
        this.corpusWriter = writer;
    }

    public void addExtraPartition(CorporaPartition partition) {
        this.extraPartitions.add(partition);
    }

    public void process(Collection<MultilingualCorpus> bilingualCorpora, Collection<Corpus> monolingualCorpora) throws ProcessingException, IOException {
        Preprocessor preprocessor = new Preprocessor(threads);

        try {
            Map<LanguagePair, Long> bilingualCorporaLinesMap = PartitioningUtils.countTotalCorporaLines(bilingualCorpora, threads);
            long extraPartitionsLines = PartitioningUtils.countTotalPartitionsLines(extraPartitions);

            for (MultilingualCorpus corpus : bilingualCorpora) {
                for (LanguagePair language : corpus.getLanguages()) {
                    long bilingualCorporaLines = bilingualCorporaLinesMap.get(language);
                    double weight = PartitioningUtils.getAdjustedWeight(language, corpus, extraPartitionsLines, bilingualCorporaLines);
                    int lineCount = corpus.getLineCount(language);

                    PreprocessingTask sourceTask = new PreprocessingTask(preprocessor, language, corpus.getCorpus(language, true), lineCount, mainPartition, corpusWriter);
                    PreprocessingTask targetTask = new PreprocessingTask(preprocessor, language.reversed(), corpus.getCorpus(language, false), lineCount, mainPartition, corpusWriter);

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

            // Enqueue monolingual corpora tasks
            if (monolingualCorpora != null) {
                for (Corpus corpus : monolingualCorpora) {
                    LanguagePair language = new LanguagePair(corpus.getLanguage(), corpus.getLanguage());
                    PreprocessingTask task = new PreprocessingTask(preprocessor, language, corpus, mainPartition, corpusWriter);
                    task.execute();
                }
            }

            corpusWriter.flush();
        } finally {
            IOUtils.closeQuietly(preprocessor);
        }
    }

}
