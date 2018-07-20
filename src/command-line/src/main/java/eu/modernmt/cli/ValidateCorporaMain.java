package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by davide on 17/12/15.
 */
public class ValidateCorporaMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(inputPath);
        }

        public final LanguagePair language;
        public final File inputRoot;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            Language sourceLanguage = Language.fromString(cli.getOptionValue('s'));
            Language targetLanguage = Language.fromString(cli.getOptionValue('t'));
            language = new LanguagePair(sourceLanguage, targetLanguage);
            inputRoot = new File(cli.getOptionValue("input"));
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);
        Args args = new Args(_args);

        List<MultilingualCorpus> corpora = Corpora.list(args.language, args.inputRoot);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CompletionService<Long> service = new ExecutorCompletionService<>(executor);

        try {
            for (MultilingualCorpus corpus : corpora) {
                service.submit(() -> {
                    MultilingualCorpus.MultilingualLineReader reader = null;

                    try {
                        long count = 0;
                        reader = corpus.getContentReader();

                        while (reader.read() != null)
                            count++;

                        return count;
                    } finally {
                        IOUtils.closeQuietly(reader);
                    }
                });
            }

            long total = 0;

            for (int i = 0; i < corpora.size(); i++) {
                total += service.take().get();
                System.out.println("Partial entries count: " + total);
            }

            System.out.println("Total entries count: " + total);
        } finally {
            executor.shutdownNow();
        }
    }

}
