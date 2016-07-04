package eu.modernmt.core.training;

import eu.modernmt.core.training.partitioning.CorporaPartition;
import eu.modernmt.core.training.partitioning.PartitionWriter;
import eu.modernmt.core.training.partitioning.PartitionedInputStream;
import eu.modernmt.model.Corpus;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.PipelineInputStream;
import eu.modernmt.processing.framework.PipelineOutputStream;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.util.TokensOutputter;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by davide on 24/02/16.
 */
class TrainingCorpusTask implements Callable<Void> {

    private Preprocessor preprocessor;

    private Corpus corpus;
    private int corpusLines;

    private CorporaPartition mainPartition;
    private List<PartitionWriter> extraPartitions = new ArrayList<>();

    public TrainingCorpusTask(Preprocessor preprocessor, Corpus corpus, int corpusLines, CorporaPartition mainPartition) {
        this.preprocessor = preprocessor;
        this.corpus = corpus;
        this.corpusLines = corpusLines;
        this.mainPartition = mainPartition;
    }

    public void addExtraPartition(CorporaPartition partition, int size) {
        extraPartitions.add(new PartitionWriter(partition, corpus, size));
    }

    @Override
    public Void call() throws ProcessingException, InterruptedException {
        PipelineInputStream<String> input = null;
        PipelineOutputStream<Sentence> output = null;

        try {
            Corpus outCorpus = mainPartition.getDestinationCorpus(this.corpus);

            if (extraPartitions.size() > 0)
                input = new PartitionedInputStream(corpus, corpusLines, extraPartitions);
            else
                input = PipelineInputStream.fromLineReader(corpus.getContentReader());

            output = new TokensOutputter(outCorpus.getContentWriter(false), false, true);

            preprocessor.process(input, output, true);
        } catch (IOException | ProcessingException e) {
            throw new ProcessingException("Failed to process corpus '" + corpus.getName() + "'", e);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);

            extraPartitions.forEach(IOUtils::closeQuietly);
        }

        return null;
    }
}
