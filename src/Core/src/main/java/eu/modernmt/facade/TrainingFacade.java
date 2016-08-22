package eu.modernmt.facade;

import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.training.partitioning.FilesCorporaPartition;
import eu.modernmt.training.preprocessing.PlainTextWriter;
import eu.modernmt.training.preprocessing.ResultWriter;
import eu.modernmt.training.preprocessing.TrainingPreprocessor;
import eu.modernmt.training.preprocessing.VocabularyEncoderWriter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 17/08/16.
 */
public class TrainingFacade {

    public static final int DEFAULT_PARTITION_SIZE = 1200;

    public static class TrainingOptions {

        public int partitionSize = DEFAULT_PARTITION_SIZE;
        public File developmentPartition = null;
        public File testPartition = null;
        public File vocabulary = null;

    }

    public void preprocess(List<BilingualCorpus> bilingualCorpora, List<Corpus> monolingualCorpora, Locale sourceLanguage,
                           Locale targetLanguage, File destFolder) throws ProcessingException, IOException {
        preprocess(bilingualCorpora, monolingualCorpora, sourceLanguage, targetLanguage, destFolder, new TrainingOptions());
    }

    public void preprocess(List<BilingualCorpus> bilingualCorpora, List<Corpus> monolingualCorpora, Locale sourceLanguage,
                           Locale targetLanguage, File destFolder, TrainingOptions options) throws ProcessingException, IOException {
        FilesCorporaPartition mainPartition = new FilesCorporaPartition(destFolder);

        ResultWriter writer;
        if (options.vocabulary == null)
            writer = new PlainTextWriter();
        else
            writer = new VocabularyEncoderWriter(options.vocabulary);

        TrainingPreprocessor preprocessor = new TrainingPreprocessor(mainPartition, sourceLanguage, targetLanguage, writer);

        preprocessor.addBilingualCorpora(bilingualCorpora);
        if (monolingualCorpora != null && !monolingualCorpora.isEmpty())
            preprocessor.addMonolingualCorpora(monolingualCorpora);

        FileUtils.deleteDirectory(destFolder);

        preprocessor.execute();
    }

//    public void preprocess(List<BilingualCorpus> bilingualCorpora, List<Corpus> monolingualCorpora, Locale sourceLanguage,
//                           Locale targetLanguage, File destFolder, TrainingOptions options) throws ProcessingException, IOException {
//        FilesCorporaPartition mainPartition = new FilesCorporaPartition(destFolder);
//        TrainingPipeline trainingPipeline = new TrainingPipeline(mainPartition, sourceLanguage, targetLanguage);
//
//        trainingPipeline.addBilingualCorpora(bilingualCorpora);
//        if (monolingualCorpora != null && !monolingualCorpora.isEmpty())
//            trainingPipeline.addMonolingualCorpora(monolingualCorpora);
//
//        FileUtils.deleteDirectory(destFolder);
//
//        if (options.developmentPartition != null) {
//            FileUtils.deleteDirectory(options.developmentPartition);
//            trainingPipeline.addExtraPartition(new FilesCorporaPartition(options.developmentPartition, options.partitionSize));
//        }
//
//        if (options.testPartition != null) {
//            FileUtils.deleteDirectory(options.testPartition);
//            trainingPipeline.addExtraPartition(new FilesCorporaPartition(options.testPartition, options.partitionSize));
//        }
//
//        if (options.vocabulary != null) {
//            FileUtils.deleteDirectory(options.vocabulary);
//            trainingPipeline.setVocabularyOutput(options.vocabulary);
//        }
//
//        try {
//            trainingPipeline.process();
//        } catch (InterruptedException e) {
//            throw new RuntimeException("Unexpected InterruptedException", e);
//        }
//    }

}
