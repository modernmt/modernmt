package eu.modernmt.cli;

import eu.modernmt.io.*;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.splitter.SentenceSplitter;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 17/12/15.
 */
public class PreprocessorMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option batch = Option.builder("b").longOpt("batch").hasArg(false).required(false).build();
            Option skipTags = Option.builder().longOpt("no-tags").hasArg(false).required(false).build();
            Option skipPlaceholders = Option.builder().longOpt("print-placeholders").hasArg(false).required(false).build();
            Option keepSpaces = Option.builder().longOpt("original-spacing").hasArg(false).required(false).build();
            Option sentenceSplit = Option.builder().longOpt("sentence-split").hasArg(false).required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(batch);
            cliOptions.addOption(skipTags);
            cliOptions.addOption(skipPlaceholders);
            cliOptions.addOption(keepSpaces);
            cliOptions.addOption(sentenceSplit);
        }

        public final LanguageDirection language;
        public final boolean printTags;
        public final boolean printPlaceholders;
        public final boolean keepSpaces;
        public final boolean batch;
        public final boolean split;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            Language source = Language.fromString(cli.getOptionValue("s"));
            Language target = Language.fromString(cli.getOptionValue("t"));
            language = new LanguageDirection(source, target);
            printTags = !cli.hasOption("no-tags");
            printPlaceholders = cli.hasOption("print-placeholders");
            keepSpaces = cli.hasOption("original-spacing");
            batch = cli.hasOption("batch");
            split = cli.hasOption("sentence-split");
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        Preprocessor preprocessor = null;
        Outputter output = null;

        LineReader input = new UnixLineReader(System.in, UTF8Charset.get());

        try {
            preprocessor = new Preprocessor();

            if (args.keepSpaces)
                output = new SentenceOutputter(args.printTags, args.printPlaceholders);
            else
                output = new TokensOutputter(args.printTags, args.printPlaceholders);

            if (args.batch)
                batchPreprocess(preprocessor, args.language, input, output, args.split);
            else
                interactivePreprocess(preprocessor, args.language, input, output, args.split);
        } finally {
            IOUtils.closeQuietly(preprocessor);
            IOUtils.closeQuietly(output);
        }
    }

    private static void interactivePreprocess(Preprocessor preprocessor, LanguageDirection language, LineReader input, Outputter output, boolean split) throws IOException, ProcessingException {
        String line;
        while ((line = input.readLine()) != null) {
            Sentence sentence = preprocessor.process(language, line);
            write(output, sentence, split);
        }
    }

    private static void batchPreprocess(Preprocessor preprocessor, LanguageDirection language, LineReader input, Outputter output, boolean split) throws IOException, ProcessingException {
        BufferedLineReader bufferedReader = new BufferedLineReader(input);

        String[] batch;
        while ((batch = bufferedReader.readLines()) != null) {
            Sentence[] tokenized = preprocessor.process(language, batch);
            for (Sentence sentence : tokenized)
                write(output, sentence, split);
        }
    }

    private static void write(Outputter outputter, Sentence sentence, boolean split) throws IOException {
        List<Sentence> sentences = null;

        if (split)
            sentences = SentenceSplitter.split(sentence);

        if (sentences == null || sentences.size() < 2) {
            outputter.write(sentence);
        } else {
            for (Sentence sentenceSplit : sentences)
                outputter.write(sentenceSplit);
        }
    }

    private interface Outputter extends Closeable {

        void write(Sentence value) throws IOException;

    }

    private static class SentenceOutputter implements Outputter {

        private final UnixLineWriter writer;
        private final boolean printTags;
        private final boolean printPlaceholders;

        public SentenceOutputter(boolean printTags, boolean printPlaceholders) {
            this.writer = new UnixLineWriter(System.out, UTF8Charset.get());
            this.printTags = printTags;
            this.printPlaceholders = printPlaceholders;
        }

        @Override
        public void write(Sentence value) throws IOException {
            writer.writeLine(value.toString(printTags, printPlaceholders));
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }

    private static class TokensOutputter implements Outputter {

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
