package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.List;

public class DeduplicationMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().build();
            Option lengthThreshold = Option.builder("l").hasArg().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(lengthThreshold);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(outputPath);
        }

        public final Language source;
        public final Language target;
        public final int lengthThreshold;
        public final File[] inputRoots;
        public final File outputRoot;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            source = Language.fromString(cli.getOptionValue('s'));
            target = cli.hasOption('t') ? Language.fromString(cli.getOptionValue('t')) : null;
            lengthThreshold = cli.hasOption("l") ? Integer.parseInt(cli.getOptionValue("l")) : 0;

            String[] roots = cli.getOptionValues("input");
            inputRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                inputRoots[i] = new File(roots[i]);

            outputRoot = new File(cli.getOptionValue("output"));
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);

        if (args.target == null) {
            List<Corpus> corpora = Corpora.list(args.source, args.inputRoots);
            if (corpora.isEmpty())
                throw new ParseException("Input path does not contains valid monolingual data");

            ModernMT.training.deduplicateMonolingual(corpora, args.outputRoot, args.lengthThreshold);
        } else {
            LanguageDirection language = new LanguageDirection(args.source, args.target);
            List<MultilingualCorpus> corpora = Corpora.list(language, args.inputRoots);
            if (corpora.isEmpty())
                throw new ParseException("Input path does not contains valid bilingual data");

            ModernMT.training.deduplicate(corpora, args.outputRoot, args.lengthThreshold);
        }
    }
}
