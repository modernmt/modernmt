package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.training.LazyWriterMultilingualCorpus;
import eu.modernmt.training.MultilingualCorpusMask;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by nicolabertoldi on 31/01/18.
 */
public class CorpusSplitMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option trainOutputPath = Option.builder().longOpt("train").hasArg().required().build();
            Option testOutputPath = Option.builder().longOpt("test").hasArg().required().build();
            Option devOutputPath = Option.builder().longOpt("dev").hasArg().build();
            Option testSize = Option.builder().longOpt("test-size").hasArg().required().build();
            Option devSize = Option.builder().longOpt("dev-size").hasArg().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(trainOutputPath);
            cliOptions.addOption(testOutputPath);
            cliOptions.addOption(devOutputPath);
            cliOptions.addOption(testSize);
            cliOptions.addOption(devSize);
        }

        public final Language sourceLanguage;
        public final Language targetLanguage;
        public final File inputRoot;
        public final File trainOutputRoot;
        public final File testOutputRoot;
        public final File devOutputRoot;
        public final int testSize;
        public final int devSize;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = Language.fromString(cli.getOptionValue('s'));
            targetLanguage = Language.fromString(cli.getOptionValue('t'));

            inputRoot = new File(cli.getOptionValue("input"));
            trainOutputRoot = new File(cli.getOptionValue("train"));
            testOutputRoot = new File(cli.getOptionValue("test"));
            testSize = Integer.parseInt(cli.getOptionValue("test-size"));
            if (cli.getOptionValue("dev") != null && (cli.getOptionValue("dev-size") != null)) {
                devOutputRoot = new File(cli.getOptionValue("dev"));
                devSize = Integer.parseInt(cli.getOptionValue("dev-size"));
            } else {
                devOutputRoot = null;
                devSize = 0;
            }
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);

        ArrayList<MultilingualCorpus> bilingualCorpora = new ArrayList<>();
        Corpora.list(null, true, bilingualCorpora, args.sourceLanguage, args.targetLanguage, args.inputRoot);

        if (bilingualCorpora.isEmpty())
            throw new ParseException("Input path does not contains valid bilingual data");

        FileUtils.deleteDirectory(args.trainOutputRoot);
        FileUtils.forceMkdir(args.trainOutputRoot);
        FileUtils.deleteDirectory(args.testOutputRoot);
        FileUtils.forceMkdir(args.testOutputRoot);
        if (args.devOutputRoot != null){
            FileUtils.deleteDirectory(args.devOutputRoot);
            FileUtils.forceMkdir(args.devOutputRoot);
        }

        LanguagePair languagePair = new LanguagePair(args.sourceLanguage, args.targetLanguage);
        LanguageIndex languageIndex = new LanguageIndex(languagePair);

        try {


            MultilingualCorpus.MultilingualLineReader inputReader = null;
            MultilingualCorpus.MultilingualLineWriter trainWriter = null;
            MultilingualCorpus.MultilingualLineWriter testWriter = null;
            MultilingualCorpus.MultilingualLineWriter devWriter = null;

            for (MultilingualCorpus _corpus : bilingualCorpora) {

                try {
                    MultilingualCorpus corpus = new MultilingualCorpusMask(languageIndex, _corpus);
                    inputReader = corpus.getContentReader();

                    trainWriter = new LazyWriterMultilingualCorpus(Corpora.rename(corpus, args.trainOutputRoot)).getContentWriter(false);
                    testWriter = new LazyWriterMultilingualCorpus(Corpora.rename(corpus, args.testOutputRoot)).getContentWriter(false);
                    if (args.devOutputRoot != null) {
                        devWriter = new LazyWriterMultilingualCorpus(Corpora.rename(corpus, args.devOutputRoot)).getContentWriter(false);
                    }

                    int corpusLength = corpus.getLineCount(languagePair);

                    if (corpusLength<=0){
                        System.err.println("Original corpus (" + _corpus+ ") has no entries (" + corpusLength+ ")");
                        continue;
                    }

                    if (args.testSize + args.devSize >= corpusLength){
                        System.err.println("Original corpus (" + _corpus+ ") does not have enough entries (" + corpusLength+ ") to split into " + args.testSize + " test sentences and " + args.devSize + " dev sentences and ");
                        continue;
                    }
                    Random randomGenerator = new Random(corpusLength);
                    ArrayList<Integer> testRandom = new ArrayList<>(args.devSize);
                    ArrayList<Integer> devRandom = new ArrayList<>(args.testSize);
                    for (int j = 0; j < args.testSize; ++j){
                        testRandom.add(randomGenerator.nextInt(corpusLength));
                    }
                    for (int j = 0; j < args.devSize; ++j) {
                        devRandom.add(randomGenerator.nextInt(corpusLength));
                    }

                    MultilingualCorpus.StringPair pair;
                    int i = 0;
                    while ((pair = inputReader.read()) != null) {
                        if (testRandom.contains(i)) {
                            testWriter.write(pair);
                        } else if (devRandom.contains(i)) {
                            devWriter.write(pair);
                        } else {
                            trainWriter.write(pair);
                        }
                        i++;
                    }

                } finally {
                    IOUtils.closeQuietly(inputReader);
                    IOUtils.closeQuietly(testWriter);
                    IOUtils.closeQuietly(devWriter);
                    IOUtils.closeQuietly(trainWriter);
                }
            }
        } finally {}
    }

}
