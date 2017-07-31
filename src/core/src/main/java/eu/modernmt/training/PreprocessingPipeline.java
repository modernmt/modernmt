package eu.modernmt.training;

import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.training.partitioning.CorporaPartition;
import eu.modernmt.training.partitioning.PartitioningUtils;
import eu.modernmt.training.preprocessing.CorpusWriter;
import eu.modernmt.training.preprocessing.TrainingPreprocessor;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by davide on 22/08/16.
 */
public class PreprocessingPipeline {

    private final int threads;
    private final CorporaPartition mainPartition;
    private final CorpusWriter corpusWriter;

    private ArrayList<CorporaPartition> extraPartitions = new ArrayList<>();

    public PreprocessingPipeline(CorporaPartition mainPartition, CorpusWriter writer) {
        this(mainPartition, writer, Runtime.getRuntime().availableProcessors());
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
        TrainingPreprocessor sourcePreprocessor = new TrainingPreprocessor(threads, sourceLanguage);
        TrainingPreprocessor targetPreprocessor = new TrainingPreprocessor(threads, targetLanguage);

        try {
            long bilingualCorporaLines = PartitioningUtils.countTotalCorporaLines(bilingualCorpora, threads);
            long extraPartitionsLines = PartitioningUtils.countTotalPartitionsLines(extraPartitions);

            for (MultilingualCorpus corpus : bilingualCorpora) {
                double weight = PartitioningUtils.getAdjustedWeight(corpus, extraPartitionsLines, bilingualCorporaLines);
                int lineCount = corpus.getLineCount();

                PreprocessingTask sourceTask = new PreprocessingTask(sourcePreprocessor, corpus.getSourceCorpus(), lineCount, mainPartition, corpusWriter);
                PreprocessingTask targetTask = new PreprocessingTask(targetPreprocessor, corpus.getTargetCorpus(), lineCount, mainPartition, corpusWriter);

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

            // Enqueue monolingual corpora tasks
            if (monolingualCorpora != null) {
                for (Corpus corpus : monolingualCorpora) {
                    PreprocessingTask task = new PreprocessingTask(targetPreprocessor, corpus, mainPartition, corpusWriter);
                    task.execute();
                }
            }

            corpusWriter.flush();
        } finally {
            IOUtils.closeQuietly(sourcePreprocessor);
            IOUtils.closeQuietly(targetPreprocessor);
        }
    }

}
