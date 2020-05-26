package eu.modernmt.facade;

import eu.modernmt.cleaning.CorporaCleaning;
import eu.modernmt.io.Corpora;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.training.BatchCopyProcess;
import eu.modernmt.training.LazyWriterCorpus;
import eu.modernmt.training.LazyWriterMultilingualCorpus;
import eu.modernmt.training.PreprocessingPipeline;
import eu.modernmt.training.bloomfilter.CorporaBloomFilter;
import eu.modernmt.training.partitioning.CorporaPartition;
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

    // - Cleaning ------------------------------------------------------------------------------------------------------

    private static class RenameCorpusFactory implements BatchCopyProcess.OutputCorpusFactory {

        private final File outputDirectory;

        public RenameCorpusFactory(File outputDirectory) {
            this.outputDirectory = outputDirectory;
        }

        @Override
        public MultilingualCorpus getOutput(MultilingualCorpus corpus) {
            return Corpora.rename(corpus, outputDirectory);
        }

        @Override
        public Corpus getOutput(Corpus corpus) {
            return Corpora.rename(corpus, outputDirectory);
        }
    }

    private static class LazyWriterFactory implements BatchCopyProcess.OutputCorpusFactory {

        private final BatchCopyProcess.OutputCorpusFactory factory;

        private LazyWriterFactory(BatchCopyProcess.OutputCorpusFactory factory) {
            this.factory = factory;
        }

        @Override
        public MultilingualCorpus getOutput(MultilingualCorpus corpus) {
            return new LazyWriterMultilingualCorpus(factory.getOutput(corpus));
        }

        @Override
        public Corpus getOutput(Corpus corpus) {
            return new LazyWriterCorpus(factory.getOutput(corpus));
        }
    }

    public void cleanMonolingual(List<Corpus> corpora, File outputDirectory, CorporaCleaning.Options options) throws IOException {
        clean(null, corpora, options, new RenameCorpusFactory(outputDirectory));
    }

    public void clean(List<MultilingualCorpus> corpora, File outputDirectory, CorporaCleaning.Options options) throws IOException {
        clean(corpora, null, options, new RenameCorpusFactory(outputDirectory));
    }

    public void clean(List<MultilingualCorpus> corpora, CorporaCleaning.Options options, BatchCopyProcess.OutputCorpusFactory factory) throws IOException {
        clean(corpora, null, options, factory);
    }

    public void clean(List<MultilingualCorpus> multilingualCorpora, List<Corpus> monolingualCorpora,
                      CorporaCleaning.Options options, BatchCopyProcess.OutputCorpusFactory factory) throws IOException {
        long sizeThreshold = DEFAULT_MAX_FILE_SIZE_PARALLEL_CLEANING;

        long maxMemory = Runtime.getRuntime().maxMemory();
        if (maxMemory < Long.MAX_VALUE)
            sizeThreshold = maxMemory / 10;

        BatchCopyProcess parallelCopyProcess = new BatchCopyProcess(new LazyWriterFactory(factory));
        BatchCopyProcess serializedCopyProcess = new BatchCopyProcess(new LazyWriterFactory(factory));
        serializedCopyProcess.setIoThreads(1);

        if (multilingualCorpora != null) {
            for (MultilingualCorpus corpus : multilingualCorpora) {
                long fileSize = Corpora.stats(corpus).size;
                corpus = CorporaCleaning.wrap(corpus, options);

                if (fileSize < sizeThreshold)
                    parallelCopyProcess.add(corpus);
                else
                    serializedCopyProcess.add(corpus);
            }
        }

        if (monolingualCorpora != null) {
            for (Corpus corpus : monolingualCorpora) {
                long fileSize = Corpora.stats(corpus).size;
                corpus = CorporaCleaning.wrap(corpus, options);

                if (fileSize < sizeThreshold)
                    parallelCopyProcess.add(corpus);
                else
                    serializedCopyProcess.add(corpus);
            }
        }

        parallelCopyProcess.run();
        serializedCopyProcess.run();
    }

    // - Pre-process ---------------------------------------------------------------------------------------------------

    public void preprocess(LanguageDirection language, List<MultilingualCorpus> corpora, File destFolder) throws ProcessingException, IOException {
        preprocess(language, corpora, destFolder, new TrainingOptions());
    }

    public void preprocess(LanguageDirection language, List<MultilingualCorpus> corpora, File destFolder, TrainingOptions options) throws ProcessingException, IOException {
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

    // - Deduplicate ---------------------------------------------------------------------------------------------------

    public void deduplicate(List<MultilingualCorpus> corpora, File outputDirectory, int lengthThreshold, boolean sorted) throws IOException {
        long lines = 0;
        for (long count : Corpora.countLines(corpora).values())
            lines += count;

        FileUtils.deleteDirectory(outputDirectory);
        FileUtils.forceMkdir(outputDirectory);

        CorporaBloomFilter bloomFilter = new CorporaBloomFilter(lines);

        BatchCopyProcess copyProcess = new BatchCopyProcess(new LazyWriterFactory(new RenameCorpusFactory(outputDirectory)));
        for (MultilingualCorpus corpus : corpora)
            copyProcess.add(bloomFilter.wrap(corpus, lengthThreshold));

        if (sorted)
            copyProcess.setIoThreads(1);

        copyProcess.run();
    }

    public void deduplicateMonolingual(List<Corpus> corpora, File outputDirectory, int lengthThreshold, boolean sorted) throws IOException {
        long lines = 0;
        for (long count : Corpora.countMonolingualLines(corpora).values())
            lines += count;

        FileUtils.deleteDirectory(outputDirectory);
        FileUtils.forceMkdir(outputDirectory);

        CorporaBloomFilter bloomFilter = new CorporaBloomFilter(lines);

        BatchCopyProcess copyProcess = new BatchCopyProcess(new LazyWriterFactory(new RenameCorpusFactory(outputDirectory)));
        for (Corpus corpus : corpora)
            copyProcess.add(bloomFilter.wrap(corpus, lengthThreshold));

        if (sorted)
            copyProcess.setIoThreads(1);

        copyProcess.run();
    }

}
