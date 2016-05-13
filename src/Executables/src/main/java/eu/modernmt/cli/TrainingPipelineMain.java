package eu.modernmt.cli;

import eu.modernmt.cli.init.Submodules;
import eu.modernmt.core.training.TrainingPipeline;
import eu.modernmt.core.training.partitioning.FilesCorporaPartition;
import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.model.Corpus;
import eu.modernmt.model.util.CorpusUtils;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class TrainingPipelineMain {

    static {
        Submodules.link();
    }

    public static final int DEFAULT_PARTITION_SIZE = 1200;

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();
            Option devPath = Option.builder().longOpt("dev").hasArg().required(false).build();
            Option testPath = Option.builder().longOpt("test").hasArg().required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(outputPath);
            cliOptions.addOption(devPath);
            cliOptions.addOption(testPath);
        }

        public final Locale sourceLanguage;
        public final Locale targetLanguage;
        public final File[] inputRoots;
        public final File outputRoot;
        public final File devRoot;
        public final File testRoot;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = Locale.forLanguageTag(cli.getOptionValue('s'));
            targetLanguage = Locale.forLanguageTag(cli.getOptionValue('t'));

            String[] roots = cli.getOptionValues("input");
            inputRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                inputRoots[i] = new File(roots[i]);

            outputRoot = new File(cli.getOptionValue("output"));

            devRoot = cli.hasOption("dev") ? new File(cli.getOptionValue("dev")) : null;
            testRoot = cli.hasOption("test") ? new File(cli.getOptionValue("test")) : null;
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        ArrayList<Corpus> monolingualCorpora = new ArrayList<>();
        ArrayList<BilingualCorpus> bilingualCorpora = new ArrayList<>();

        CorpusUtils.list(monolingualCorpora, true, bilingualCorpora, args.sourceLanguage, args.targetLanguage, args.inputRoots);

        if (bilingualCorpora.isEmpty())
            throw new ParseException("Input path does not contains valid bilingual data");

        FilesCorporaPartition mainPartition = new FilesCorporaPartition(args.outputRoot);
        TrainingPipeline trainingPipeline = new TrainingPipeline(mainPartition, args.sourceLanguage, args.targetLanguage);

        trainingPipeline.addBilingualCorpora(bilingualCorpora);
        if (!monolingualCorpora.isEmpty())
            trainingPipeline.addMonolingualCorpora(monolingualCorpora);

        FileUtils.deleteDirectory(args.outputRoot);

        if (args.devRoot != null) {
            FileUtils.deleteDirectory(args.devRoot);
            trainingPipeline.addExtraPartition(new FilesCorporaPartition(args.devRoot, DEFAULT_PARTITION_SIZE));
        }

        if (args.testRoot != null) {
            FileUtils.deleteDirectory(args.testRoot);
            trainingPipeline.addExtraPartition(new FilesCorporaPartition(args.testRoot, DEFAULT_PARTITION_SIZE));
        }

        trainingPipeline.process();
    }

}
