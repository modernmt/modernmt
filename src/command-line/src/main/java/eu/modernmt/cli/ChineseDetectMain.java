package eu.modernmt.cli;

import eu.modernmt.cleaning.detect.ChineseDetector;
import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.lang.Language2;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ChineseDetectMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(inputPath);
            cliOptions.addOption(outputPath);
        }

        public final File inputRoot;
        public final File outputPath;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            inputRoot = new File(cli.getOptionValue("input"));
            outputPath = new File(cli.getOptionValue("output"));
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);

        List<Corpus> corpora = Corpora.list(Language2.CHINESE, args.inputRoot);

        if (corpora.isEmpty())
            throw new ParseException("Input path does not contains valid chinese corpora");

        ChineseDetector detector = ChineseDetector.getInstance();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            List<Future<Void>> results = new ArrayList<>(corpora.size());
            for (Corpus corpus : corpora) {
                File outputFile = new File(args.outputPath, corpus.getName() + ".labels");
                FileCorpus output = new FileCorpus(outputFile, corpus.getName(), Language2.CHINESE);

                results.add(executor.submit(new DetectTask(detector, corpus, output)));
            }

            for (Future<Void> result : results)
                result.get();
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class DetectTask implements Callable<Void> {

        private final ChineseDetector detector;
        private final Corpus corpus;
        private final Corpus output;

        public DetectTask(ChineseDetector detector, Corpus corpus, Corpus output) {
            this.detector = detector;
            this.corpus = corpus;
            this.output = output;
        }


        @Override
        public Void call() throws Exception {
            LineReader reader = null;
            LineWriter writer = null;

            try {
                reader = corpus.getContentReader();
                writer = output.getContentWriter(false);

                String line;
                while ((line = reader.readLine()) != null) {
                    Language2 language = detector.detect(line);
                    writer.writeLine(language.toLanguageTag());
                }
            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(writer);
            }

            return null;
        }
    }

}
