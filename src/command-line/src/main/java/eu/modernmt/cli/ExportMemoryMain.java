package eu.modernmt.cli;

import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.TUWriter;
import eu.modernmt.model.corpus.TranslationUnit;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ExportMemoryMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option model = Option.builder("m").longOpt("model").hasArg().required().build();
            Option id = Option.builder("i").longOpt("id").hasArg().required().build();
            Option output = Option.builder("o").longOpt("output-prefix").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(model);
            cliOptions.addOption(id);
            cliOptions.addOption(output);
        }

        public final File model;
        public final long id;
        public final File outputPrefix;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            this.model = new File(cli.getOptionValue("model"));
            this.id = Long.parseLong(cli.getOptionValue("id"));
            this.outputPrefix = new File(cli.getOptionValue("output-prefix"));
        }
    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        LuceneTranslationMemory memory = new LuceneTranslationMemory(args.model, 1);
        final MultiWriter writer = new MultiWriter(args.outputPrefix);

        try {
            memory.dump(args.id, e -> {
                try {
                    writer.write(new TranslationUnit(e.language, e.sentence, e.translation));
                } catch (IOException ex) {
                    throw new RuntimeIOException(ex);
                }
            });
        } finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(memory);
        }
    }

    private static class MultiWriter implements TUWriter {

        private final File folder;
        private final String prefix;
        private final HashMap<LanguageDirection, TUWriter> writers = new HashMap<>();

        public MultiWriter(File prefix) {
            this.folder = prefix.getParentFile();
            this.prefix = prefix.getName();
        }

        private TUWriter getWriter(LanguageDirection language) {
            return writers.computeIfAbsent(language, l -> {
                String filename = prefix + '_' + language.source.getLanguage() + '_' + language.target.getLanguage() + ".tmx";
                TMXCorpus tmx = new TMXCorpus(new File(folder, filename));
                try {
                    return tmx.getContentWriter(false);
                } catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
            });
        }

        @Override
        public void write(TranslationUnit tu) throws IOException {
            try {
                TUWriter writer = getWriter(tu.language);
                writer.write(tu);
            } catch (RuntimeIOException e) {
                throw e.getCause();
            }
        }

        @Override
        public void flush() throws IOException {
            for (TUWriter writer : writers.values())
                writer.flush();
        }

        @Override
        public void close() throws IOException {
            for (TUWriter writer : writers.values())
                writer.close();
        }
    }

}
