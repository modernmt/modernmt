package eu.modernmt.engine.training.partitioning;

import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.model.Corpus;
import eu.modernmt.processing.framework.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Created by davide on 11/02/16.
 */
public class PreprocessorTask implements Callable<Void> {

    private ProcessingPipeline<String, String> sourcePipeline;
    private ProcessingPipeline<String, String> targetPipeline;
    private BilingualCorpus corpus;
    private CorporaPartition mainPartition;
    private List<PartitionWriter> extraPartitions = new ArrayList<>();

    public PreprocessorTask(BilingualCorpus corpus, CorporaPartition mainPartition, ProcessingPipeline<String, String> sourcePipeline, ProcessingPipeline<String, String> targetPipeline) {
        this.corpus = corpus;
        this.mainPartition = mainPartition;
        this.targetPipeline = targetPipeline;
        this.sourcePipeline = sourcePipeline;
    }

    public void addExtraPartition(CorporaPartition partition, int size) {
        extraPartitions.add(new PartitionWriter(partition, corpus, size));
    }

    @Override
    public Void call() throws InterruptedException, ProcessingException {
        PipelineInputStream<String> sourceInput = null;
        PipelineOutputStream<String> sourceOutput = null;
        PipelineInputStream<String> targetInput = null;
        PipelineOutputStream<String> targetOutput = null;

        try {
            BilingualCorpus outBilingualCorpus = mainPartition.getDestinationParallelCorpus(corpus);
            Corpus outSourceCorpus = outBilingualCorpus.getSourceCorpus();
            Corpus outTargetCorpus = outBilingualCorpus.getTargetCorpus();

            Locale sourceLanguage = corpus.getSourceLanguage();
            Locale targetLanguage = corpus.getTargetLanguage();

            int lines = corpus.getLineCount();

            sourceInput = new PartitionedInputStream(outSourceCorpus, lines, sourceLanguage, extraPartitions);
            targetInput = new PartitionedInputStream(outTargetCorpus, lines, targetLanguage, extraPartitions);
            sourceOutput = PipelineOutputStream.fromWriter(outSourceCorpus.getContentWriter(false));
            targetOutput = PipelineOutputStream.fromWriter(outTargetCorpus.getContentWriter(false));

            ProcessingJob<String, String> sourceJob = sourcePipeline.createJob(sourceInput, sourceOutput);
            ProcessingJob<String, String> targetJob = targetPipeline.createJob(targetInput, targetOutput);

            sourceJob.start();
            targetJob.start();

            sourceJob.join();
            targetJob.join();
        } catch (IOException e) {
            throw new ProcessingException(e);
        } finally {
            IOUtils.closeQuietly(sourceInput);
            IOUtils.closeQuietly(targetInput);
            IOUtils.closeQuietly(sourceOutput);
            IOUtils.closeQuietly(targetOutput);

            extraPartitions.forEach(IOUtils::closeQuietly);
        }

        return null;
    }

}
