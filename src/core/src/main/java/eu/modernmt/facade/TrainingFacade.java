package eu.modernmt.facade;

import eu.modernmt.cleaning.CorporaCleaning;
import eu.modernmt.io.IOCorporaUtils;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.training.BatchCopyProcess;
import eu.modernmt.training.LazyWriterMultilingualCorpus;
import eu.modernmt.training.PreprocessingPipeline;
import eu.modernmt.training.filters.CorporaBloomFilter;
import eu.modernmt.training.partitioning.CorporaPartition;
import eu.modernmt.training.AsyncCorpusWriter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 17/08/16.
 */
public class TrainingFacade {

    private static final int DEFAULT_PARTITION_SIZE = 2000;
    private static final long DEFAULT_MAX_FILE_SIZE_PARALLEL_CLEANING = 2L * 1024L * 1024L * 1024L; // 2Gb

    public static class TrainingOptions {

        public int partitionSize = DEFAULT_PARTITION_SIZE;
        public File developmentPartition = null;
        public File testPartition = null;

    }

    public void clean(List<MultilingualCorpus> corpora, File outputDirectory, CorporaCleaning.Options options) throws IOException {
        this.clean(corpora, outputDirectory, options, corpus -> Corpora.rename(corpus, outputDirectory));
    }

    public void clean(List<MultilingualCorpus> corpora, File outputDirectory, CorporaCleaning.Options options, BatchCopyProcess.OutputCorpusFactory factory) throws IOException {
        long sizeThreshold = DEFAULT_MAX_FILE_SIZE_PARALLEL_CLEANING;

        long maxMemory = Runtime.getRuntime().maxMemory();
        if (maxMemory < Long.MAX_VALUE)
            sizeThreshold = maxMemory / 10;

        BatchCopyProcess parallelCopyProcess = new BatchCopyProcess(c -> new LazyWriterMultilingualCorpus(factory.getOutput(c)));
        BatchCopyProcess serializedCopyProcess = new BatchCopyProcess(c -> new LazyWriterMultilingualCorpus(factory.getOutput(c)));
        serializedCopyProcess.setIoThreads(1);

        for (MultilingualCorpus corpus : corpora) {
            long fileSize = Corpora.fileSize(corpus);
            corpus = CorporaCleaning.wrap(corpus, options);

            if (fileSize < sizeThreshold)
                parallelCopyProcess.add(corpus);
            else
                serializedCopyProcess.add(corpus);
        }

        FileUtils.deleteDirectory(outputDirectory);
        FileUtils.forceMkdir(outputDirectory);

        parallelCopyProcess.run();
        serializedCopyProcess.run();
    }

    public void preprocess(LanguagePair language, List<MultilingualCorpus> corpora, File destFolder) throws ProcessingException, IOException {
        preprocess(language, corpora, destFolder, new TrainingOptions());
    }

    public void preprocess(LanguagePair language, List<MultilingualCorpus> corpora, File destFolder, TrainingOptions options) throws ProcessingException, IOException {
        CorporaPartition mainPartition = new CorporaPartition(destFolder);
        PreprocessingPipeline pipeline = new PreprocessingPipeline(language, mainPartition);

        FileUtils.deleteDirectory(destFolder);

        if (options.developmentPartition != null) {
            FileUtils.deleteDirectory(options.developmentPartition);
            pipeline.addExtraPartition(new CorporaPartition(options.developmentPartition, options.partitionSize));
        }

        if (options.testPartition != null) {
            FileUtils.deleteDirectory(options.testPartition);
            pipeline.addExtraPartition(new CorporaPartition(options.testPartition, options.partitionSize));
        }

        pipeline.process(corpora);
    }

    public void deduplicate(List<MultilingualCorpus> corpora, File outputDirectory, int lengthThreshold) throws IOException {
        long lines = 0;
        for (long count : IOCorporaUtils.countLines(corpora).values())
            lines += count;

        FileUtils.deleteDirectory(outputDirectory);
        FileUtils.forceMkdir(outputDirectory);

        CorporaBloomFilter bloomFilter = new CorporaBloomFilter(lines);

        BatchCopyProcess copyProcess = new BatchCopyProcess(corpus ->
                new LazyWriterMultilingualCorpus(bloomFilter.wrap(Corpora.rename(corpus, outputDirectory), lengthThreshold)));
        copyProcess.addAll(corpora);
        copyProcess.run();
    }

}
