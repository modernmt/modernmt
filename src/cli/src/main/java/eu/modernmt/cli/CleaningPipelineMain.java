package eu.modernmt.cli;

import eu.modernmt.cli.init.Submodules;
import eu.modernmt.training.CleaningPipeline;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.Corpora;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class CleaningPipelineMain {

    static {
        Submodules.link();
    }

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(outputPath);
        }

        public final Locale sourceLanguage;
        public final Locale targetLanguage;
        public final File[] inputRoots;
        public final File outputRoot;

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
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        ArrayList<BilingualCorpus> bilingualCorpora = new ArrayList<>();
        Corpora.list(null, true, bilingualCorpora, args.sourceLanguage, args.targetLanguage, args.inputRoots);

        if (bilingualCorpora.isEmpty())
            throw new ParseException("Input path does not contains valid bilingual data");

        CleaningPipeline cleaningPipeline = new CleaningPipeline(corpus -> new ParallelFileCorpus(args.outputRoot, corpus.getName(), args.sourceLanguage, args.targetLanguage), args.sourceLanguage, args.targetLanguage);
        bilingualCorpora.forEach(cleaningPipeline::add);

        FileUtils.deleteDirectory(args.outputRoot);
        FileUtils.forceMkdir(args.outputRoot);
        cleaningPipeline.process();
    }

}
