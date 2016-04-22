package eu.modernmt.core.training;

import eu.modernmt.core.training.partitioning.CorporaPartition;
import eu.modernmt.core.training.partitioning.PartitionWriter;
import eu.modernmt.core.training.partitioning.PartitionedInputStream;
import eu.modernmt.model.Corpus;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.framework.*;
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

    private ProcessingPipeline<String, Sentence> pipeline;

    private Corpus corpus;
    private int corpusLines;

    private CorporaPartition mainPartition;
    private List<PartitionWriter> extraPartitions = new ArrayList<>();

    public TrainingCorpusTask(ProcessingPipeline<String, Sentence> pipeline, Corpus corpus, int corpusLines, CorporaPartition mainPartition) {
        this.pipeline = pipeline;
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
                input = PipelineInputStream.fromReader(corpus.getContentReader());

            output = new TokensOutputter(outCorpus.getContentWriter(false), false, true);

            ProcessingJob<String, Sentence> job = pipeline.createJob(input, output);

            job.start();
            job.join();
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
