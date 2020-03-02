package eu.modernmt.cli;

import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MemoryExportMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option id = Option.builder().longOpt("id").hasArg().required().build();
            Option memory = Option.builder().longOpt("memory").hasArg().required().build();
            Option output = Option.builder().longOpt("output").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(id);
            cliOptions.addOption(memory);
            cliOptions.addOption(output);
        }

        public final long id;
        public final File memoryFolder;
        public final File outputFolder;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            id = Long.parseLong(cli.getOptionValue("id"));
            memoryFolder = new File(cli.getOptionValue("memory"));
            outputFolder = new File(cli.getOptionValue("output"));
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        FileUtils.forceMkdir(args.outputFolder);
        HashMap<String, MultilingualCorpus.MultilingualLineWriter> writers = new HashMap<>();

        LuceneTranslationMemory memory = new LuceneTranslationMemory(args.memoryFolder, 1);
        memory.dump(args.id, entry -> {
            String key = toKey(entry.language);
            MultilingualCorpus.MultilingualLineWriter writer = writers.computeIfAbsent(key, k -> {
                TMXCorpus corpus = new TMXCorpus(getFilename(args.outputFolder, args.id, key));
                try {
                    return corpus.getContentWriter(false);
                } catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
            });

            MultilingualCorpus.StringPair pair = new MultilingualCorpus.StringPair(entry.language, entry.sentence, entry.translation);
            try {
                writer.write(pair);
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        });

        for (MultilingualCorpus.MultilingualLineWriter writer : writers.values()) {
            writer.close();
        }
    }

    private static File getFilename(File outputFolder, long id, String key) {
        return new File(outputFolder, id + "_" + key + ".tmx");
    }

    private static String toKey(LanguageDirection direction) {
        return direction.source.toLanguageTag(false, true) + "__" + direction.target.getLanguage();
    }

}
