package eu.modernmt.cli;

import eu.modernmt.io.*;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.PipelineInputStream;
import eu.modernmt.processing.PipelineOutputStream;
import eu.modernmt.processing.Preprocessor;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
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

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        Preprocessor preprocessor = null;
        PipelineInputStream<String> input = null;
        PipelineOutputStream<Sentence> output = null;

        LineReader sin = new UnixLineReader(System.in, DefaultCharset.get());

        try {
            preprocessor = new Preprocessor(args.language);

            input = PipelineInputStream.fromLineReader(sin);
            if (args.keepSpaces)
                output = new SentenceOutputter(args.printTags, args.printPlaceholders);
            else
                output = new TokensOutputter(args.printTags, args.printPlaceholders);

            preprocessor.process(input, output);
        } finally {
            IOUtils.closeQuietly(preprocessor);
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
    }

    private static class SentenceOutputter implements PipelineOutputStream<Sentence> {

        private final SentenceOutputStream stream;

        public SentenceOutputter(boolean printTags, boolean printPlaceholders) {
            stream = new SentenceOutputStream(System.out, printTags, printPlaceholders);
        }

        @Override
        public void write(Sentence value) throws IOException {
            stream.write(value);
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

    private static class TokensOutputter implements PipelineOutputStream<Sentence> {

        private final TokensOutputStream stream;

        public TokensOutputter(boolean printTags, boolean printPlaceholders) {
            stream = new TokensOutputStream(System.out, printTags, printPlaceholders);
        }

        @Override
        public void write(Sentence value) throws IOException {
            stream.write(value);
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

}
