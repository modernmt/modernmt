package eu.modernmt.facade;

import eu.modernmt.cleaning.CorporaCleaning;
import eu.modernmt.engine.Engine;
import eu.modernmt.io.IOCorporaUtils;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.training.*;
import eu.modernmt.training.partitioning.FilesCorporaPartition;
import eu.modernmt.training.preprocessing.CorpusWriter;
import eu.modernmt.training.preprocessing.PlainTextWriter;
import eu.modernmt.training.preprocessing.TermsCollectorWriter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by davide on 17/08/16.
 */
public class TrainingFacade {

    static {
        Engine.initialize();
    }

    public static final int DEFAULT_PARTITION_SIZE = 2000;

    public static class TrainingOptions {

        public int partitionSize = DEFAULT_PARTITION_SIZE;
        public File developmentPartition = null;
        public File testPartition = null;
        public File vocabulary = null;

    }

    public void clean(LanguageIndex languages, List<MultilingualCorpus> corpora, File outputDirectory, CorporaCleaning.Options options) throws IOException {
        BatchCopyProcess copyProcess = new BatchCopyProcess(corpus -> new LazyWriterMultilingualCorpus(Corpora.rename(corpus, outputDirectory)));

        for (MultilingualCorpus corpus : corpora)
            copyProcess.add(CorporaCleaning.wrap(new MultilingualCorpusMask(languages, corpus), options));

        FileUtils.deleteDirectory(outputDirectory);
        FileUtils.forceMkdir(outputDirectory);

        copyProcess.run();
    }

    public void preprocess(LanguageIndex languages, List<MultilingualCorpus> multilingualCorpora, List<Corpus> monolingualCorpora, File destFolder) throws ProcessingException, IOException {
        preprocess(languages, multilingualCorpora, monolingualCorpora, destFolder, new TrainingOptions());
    }

    public void preprocess(LanguageIndex languages, List<MultilingualCorpus> multilingualCorpora, List<Corpus> monolingualCorpora, File destFolder, TrainingOptions options) throws ProcessingException, IOException {
        FilesCorporaPartition mainPartition = new FilesCorporaPartition(destFolder);

        CorpusWriter writer;
        if (options.vocabulary == null)
            writer = new PlainTextWriter();
        else
            writer = new TermsCollectorWriter(options.vocabulary);

        PreprocessingPipeline pipeline = new PreprocessingPipeline(languages, mainPartition, writer);

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

    public void reduce(LanguageIndex languages, List<MultilingualCorpus> originalCorpora, File outputDirectory, long maxWordCount) throws IOException {
        ArrayList<ReducedMultilingualCorpus> corpora = new ArrayList<>(originalCorpora.size());
        for (MultilingualCorpus corpus : originalCorpora)
            corpora.add(new ReducedMultilingualCorpus(new MultilingualCorpusMask(languages, corpus)));

        Map<LanguagePair, Long> counts = IOCorporaUtils.wordCount(corpora, Runtime.getRuntime().availableProcessors());

        for (Map.Entry<LanguagePair, Long> e : counts.entrySet()) {
            LanguagePair language = e.getKey();
            long count = e.getValue();

            if (count > maxWordCount) {
                double reduction = maxWordCount / ((double) count);
                for (ReducedMultilingualCorpus corpus : corpora)
                    corpus.reduce(language, reduction);
            }
        }

        FileUtils.deleteDirectory(outputDirectory);
        FileUtils.forceMkdir(outputDirectory);

        BatchCopyProcess copyProcess = new BatchCopyProcess(corpus -> Corpora.rename(corpus, outputDirectory));
        copyProcess.addAll(corpora);
        copyProcess.run();
    }

}
