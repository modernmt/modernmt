package eu.modernmt.facade;

import eu.modernmt.cleaning.FilteredMultilingualCorpus;
import eu.modernmt.engine.Engine;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.training.CleaningPipeline;
import eu.modernmt.training.PreprocessingPipeline;
import eu.modernmt.training.partitioning.FilesCorporaPartition;
import eu.modernmt.training.preprocessing.CorpusWriter;
import eu.modernmt.training.preprocessing.PlainTextWriter;
import eu.modernmt.training.preprocessing.TermsCollectorWriter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 17/08/16.
 */
public class TrainingFacade {

    static {
        Engine.initialize();
    }

    public static final int DEFAULT_PARTITION_SIZE = 1200;

    public static class TrainingOptions {

        public int partitionSize = DEFAULT_PARTITION_SIZE;
        public File developmentPartition = null;
        public File testPartition = null;
        public File vocabulary = null;

    }

    public void clean(List<MultilingualCorpus> bilingualCorpora, File outputDirectory) throws IOException {
        CleaningPipeline cleaningPipeline = new CleaningPipeline(corpus -> {
            while (corpus instanceof FilteredMultilingualCorpus) {
                corpus = ((FilteredMultilingualCorpus) corpus).getWrappedCorpus();
            }

            return Corpora.rename(corpus, outputDirectory, corpus.getName());
        });
        bilingualCorpora.forEach(cleaningPipeline::add);

        FileUtils.deleteDirectory(outputDirectory);
        FileUtils.forceMkdir(outputDirectory);

        cleaningPipeline.process();
    }

    public void preprocess(List<MultilingualCorpus> multilingualCorpora, List<Corpus> monolingualCorpora, File destFolder) throws ProcessingException, IOException {
        preprocess(multilingualCorpora, monolingualCorpora, destFolder, new TrainingOptions());
    }

    public void preprocess(List<MultilingualCorpus> multilingualCorpora, List<Corpus> monolingualCorpora, File destFolder, TrainingOptions options) throws ProcessingException, IOException {
        FilesCorporaPartition mainPartition = new FilesCorporaPartition(destFolder);

        CorpusWriter writer;
        if (options.vocabulary == null)
            writer = new PlainTextWriter();
        else
            writer = new TermsCollectorWriter(options.vocabulary);

        PreprocessingPipeline pipeline = new PreprocessingPipeline(mainPartition, writer);

        FileUtils.deleteDirectory(destFolder);

        if (options.developmentPartition != null) {
            FileUtils.deleteDirectory(options.developmentPartition);
            pipeline.addExtraPartition(new FilesCorporaPartition(options.developmentPartition, options.partitionSize));
        }

        if (options.testPartition != null) {
            FileUtils.deleteDirectory(options.testPartition);
            pipeline.addExtraPartition(new FilesCorporaPartition(options.testPartition, options.partitionSize));
        }

        pipeline.process(multilingualCorpora, monolingualCorpora);
    }

}
