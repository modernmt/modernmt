package eu.modernmt.cli;

import eu.modernmt.io.*;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PostprocessorMain {

    private static final Sentence EMPTY_SENTENCE = new Sentence(new Word[0]);
    private static final Alignment EMPTY_ALIGNMENT = new Alignment(new int[0], new int[0]);

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option srcInputPath = Option.builder().longOpt("source").hasArg().build();
            Option alignInputPath = Option.builder().longOpt("alignment").hasArg().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(srcInputPath);
            cliOptions.addOption(alignInputPath);

        }

        public final LanguageDirection language;
        public final File source;
        public final File alignment;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            Language _source = Language.fromString(cli.getOptionValue('s'));
            Language _target = cli.hasOption('t') ? Language.fromString(cli.getOptionValue('t')) : null;
            language = new LanguageDirection(_source, _target);

            source = cli.hasOption("source") ? new File(cli.getOptionValue("source")) : null;
            alignment = cli.hasOption("alignment") ? new File(cli.getOptionValue("alignment")) : null;

            if ((source == null && alignment != null) || (source != null && alignment == null))
                throw new ParseException("You must specify both '--source' and '--alignment' options or none of them");
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        TranslationProvider translations = null;
        Postprocessor postprocessor = null;
        LineWriter stdout = null;

        try {
            translations = new TranslationProvider(args.language, args.source, args.alignment);
            postprocessor = new Postprocessor();
            stdout = new UnixLineWriter(System.out, UTF8Charset.get());

            Translation translation;

            while ((translation = translations.next()) != null) {
                postprocessor.process(args.language, translation);
                stdout.writeLine(translation.toString());
            }
        } finally {
            IOUtils.closeQuietly(translations);
            IOUtils.closeQuietly(postprocessor);
            IOUtils.closeQuietly(stdout);
        }
    }

    private static class TranslationProvider implements Closeable {

        private final LanguageDirection language;
        private final Preprocessor preprocessor;
        private final LineReader alignments;
        private final LineReader sources;
        private final LineReader stdin;

        public TranslationProvider(LanguageDirection language, File source, File alignment) throws IOException {
            this.language = language;

            if (source != null && alignment != null) {
                boolean success = false;

                try {
                    this.preprocessor = new Preprocessor();
                    this.alignments = new UnixLineReader(new FileInputStream(alignment), UTF8Charset.get());
                    this.sources = new UnixLineReader(new FileInputStream(source), UTF8Charset.get());
                    success = true;
                } finally {
                    if (!success)
                        close();
                }
            } else {
                this.preprocessor = null;
                this.alignments = null;
                this.sources = null;
            }

            this.stdin = new UnixLineReader(System.in, UTF8Charset.get());
        }

        public Translation next() throws IOException {
            String target = stdin.readLine();
            if (target == null)
                return null;

            Sentence sentence = EMPTY_SENTENCE;
            Alignment alignment = EMPTY_ALIGNMENT;
            Word[] words = TokensOutputStream.deserializeWords(target);

            if (sources != null) {
                String sourceStr = sources.readLine();
                String alignmentStr = alignments.readLine();
                if (sourceStr == null || alignmentStr == null)
                    throw new IOException("Unexpected EOF");

                try {
                    sentence = preprocessor.process(language, sourceStr);
                } catch (ProcessingException e) {
                    throw new IOException(e);
                }

                alignment = parseAlignment(alignmentStr);
            }

            return new Translation(words, sentence, alignment);
        }

        private static Alignment parseAlignment(String string) {
            String[] parts = string.split("\\s+");
            int[] sourceIndexes = new int[parts.length];
            int[] targetIndexes = new int[parts.length];

            for (int i = 0; i < parts.length; ++i) {
                String[] st = parts[i].split("-", 2);
                sourceIndexes[i] = Integer.parseInt(st[0]);
                targetIndexes[i] = Integer.parseInt(st[1]);
            }

            return new Alignment(sourceIndexes, targetIndexes);
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(preprocessor);
            IOUtils.closeQuietly(alignments);
            IOUtils.closeQuietly(sources);
            IOUtils.closeQuietly(stdin);
        }
    }

}
