package eu.modernmt.cli;

import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.*;
import eu.modernmt.processing.util.SentenceOutputter;
import eu.modernmt.processing.util.TokensOutputter;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class PreprocessorMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option lang = Option.builder().longOpt("lang").hasArg().required().build();
            Option skipTags = Option.builder().longOpt("no-tags").hasArg(false).required(false).build();
            Option skipPlaceholders = Option.builder().longOpt("print-placeholders").hasArg(false).required(false).build();
            Option keepSpaces = Option.builder().longOpt("original-spacing").hasArg(false).required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(lang);
            cliOptions.addOption(skipTags);
            cliOptions.addOption(skipPlaceholders);
            cliOptions.addOption(keepSpaces);
        }

        public final Locale language;
        public final boolean printTags;
        public final boolean printPlaceholders;
        public final boolean keepSpaces;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            language = Locale.forLanguageTag(cli.getOptionValue("lang"));
            printTags = !cli.hasOption("no-tags");
            printPlaceholders = cli.hasOption("print-placeholders");
            keepSpaces = cli.hasOption("original-spacing");
        }

    }

    public static void main(String[] _args) throws InterruptedException, ProcessingException, ParseException {
        Args args = new Args(_args);

        ProcessingPipeline<String, Sentence> pipeline = null;
        PipelineInputStream<String> input = null;
        PipelineOutputStream<Sentence> output = null;

        try {
            pipeline = Preprocessor.getPipeline(args.language, true);

            input = PipelineInputStream.fromInputStream(System.in);
            if (args.keepSpaces)
                output = new SentenceOutputter(System.out, args.printTags, args.printPlaceholders);
            else
                output = new TokensOutputter(System.out, args.printTags, args.printPlaceholders);

            ProcessingJob<String, Sentence> job = pipeline.createJob(input, output);

            job.start();
            job.join();
        } finally {
            IOUtils.closeQuietly(pipeline);
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
    }

}
