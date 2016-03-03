package eu.modernmt.cli;

import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.PipelineInputStream;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.ProcessingJob;
import eu.modernmt.processing.framework.ProcessingPipeline;
import eu.modernmt.processing.util.SentenceOutputter;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class TokenizerMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option lang = Option.builder().longOpt("lang").hasArg().required().build();
            Option skipTags = Option.builder().longOpt("no-tags").hasArg(false).required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(lang);
            cliOptions.addOption(skipTags);
        }

        public final Locale language;
        public final boolean printTags;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            language = Locale.forLanguageTag(cli.getOptionValue("lang"));
            printTags = !cli.hasOption("no-tags");
        }

    }

    public static void main(String[] _args) throws InterruptedException, ProcessingException, ParseException {
        Args args = new Args(_args);

        ProcessingPipeline<String, Sentence> pipeline = null;

        try {
            pipeline = Preprocessor.getPipeline(args.language, true);

            ProcessingJob<String, Sentence> job = pipeline.createJob(
                    PipelineInputStream.fromInputStream(System.in),
                    new SentenceOutputter(System.out, args.printTags)
            );

            job.start();
            job.join();

        } finally {
            IOUtils.closeQuietly(pipeline);
        }
    }

}
