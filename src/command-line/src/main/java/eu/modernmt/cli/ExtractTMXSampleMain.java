package eu.modernmt.cli;

import eu.modernmt.lang.Language2;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.MaskedMultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ExtractTMXSampleMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option input = Option.builder("i").hasArg().required().build();
            Option outputFolder = Option.builder("o").hasArg().required().build();
            Option lines = Option.builder("n").hasArg().required(false).build();
            Option minLength = Option.builder("l").hasArg().required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(input);
            cliOptions.addOption(outputFolder);
            cliOptions.addOption(lines);
            cliOptions.addOption(minLength);
        }

        public final LanguageDirection language;
        public final File input;
        public final File outputFolder;
        public final int samples;
        public final int minLength;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            Language2 source = Language2.fromString(cli.getOptionValue("s"));
            Language2 target = Language2.fromString(cli.getOptionValue("t"));
            language = new LanguageDirection(source, target);
            input = new File(cli.getOptionValue("i"));
            outputFolder = new File(cli.getOptionValue("o"));
            samples = cli.hasOption("n") ? Integer.parseInt(cli.getOptionValue("n")) : 1000;
            minLength = cli.hasOption("l") ? Integer.parseInt(cli.getOptionValue("l")) : 20;
        }

    }

    private static Set<Integer> selectIndexes(MultilingualCorpus corpus, int samples, int minLength) throws IOException {
        List<Integer> indexes = new ArrayList<>();
        MultilingualCorpus.MultilingualLineReader reader = null;

        try {
            reader = corpus.getContentReader();

            int index = 0;

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                if (pair.source.length() > minLength)
                    indexes.add(index);
                index++;
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }

        Collections.shuffle(indexes);
        return new HashSet<>(indexes.subList(0, samples));
    }

    private static void extractTest(MultilingualCorpus corpus, MultilingualCorpus test, int samples, int minLength) throws IOException {
        Set<Integer> indexes = selectIndexes(corpus, samples, minLength);

        MultilingualCorpus.MultilingualLineReader reader = null;
        MultilingualCorpus.MultilingualLineWriter writer = null;

        try {
            reader = corpus.getContentReader();
            writer = test.getContentWriter(false);

            int index = 0;

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                if (indexes.contains(index))
                    writer.write(pair);

                index++;
            }
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

    private static String toKey(MultilingualCorpus.StringPair pair) {
        return pair.source.replaceAll("\\s+", "") +
                '\n' +
                pair.target.replaceAll("\\s+", "");
    }

    private static Set<String> loadTestSet(MultilingualCorpus corpus) {
        HashSet<String> set = new HashSet<>();
        MultilingualCorpus.MultilingualLineReader reader = null;

        try {
            reader = corpus.getContentReader();

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null)
                set.add(toKey(pair));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return set;
    }

    private static void filterTrain(MultilingualCorpus corpus, MultilingualCorpus test, MultilingualCorpus output) throws IOException {
        Set<String> testSet = loadTestSet(test);

        MultilingualCorpus.MultilingualLineReader reader = null;
        MultilingualCorpus.MultilingualLineWriter writer = null;

        try {
            reader = corpus.getContentReader();
            writer = output.getContentWriter(false);

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                if (!testSet.contains(toKey(pair)))
                    writer.write(pair);
            }
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        if (!args.outputFolder.isDirectory())
            FileUtils.forceMkdir(args.outputFolder);

        File testFolder = new File(args.outputFolder, "test");
        if (!testFolder.isDirectory())
            FileUtils.forceMkdir(testFolder);

        MultilingualCorpus origin = new MaskedMultilingualCorpus(args.language, new TMXCorpus(args.input));
        ParallelFileCorpus test = new ParallelFileCorpus(testFolder, origin.getName(), args.language);
        TMXCorpus train = new TMXCorpus(new File(args.outputFolder, origin.getName() + ".tmx"));

        extractTest(origin, test, args.samples, args.minLength);
        filterTrain(origin, test, train);
    }

}