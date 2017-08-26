package eu.modernmt.cli;

import eu.modernmt.cleaning.FilteredMultilingualCorpus;
import eu.modernmt.cleaning.filters.EmptyLinesFilter;
import eu.modernmt.cleaning.filters.RareNgramFilter;
import eu.modernmt.cleaning.filters.SentenceLengthFilter;
import eu.modernmt.cleaning.filters.draft.DraftFilter;
import eu.modernmt.cleaning.filters.util.WordCounter;
import eu.modernmt.cleaning.normalizers.ControlCharsStripper;
import eu.modernmt.cleaning.normalizers.XMLStripper;
import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.io.IOCorporaUtils;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.training.preprocessing.MultilingualCorpusMask;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by davide on 17/12/15.
 */
public class NeuralCleaningTestMain {

    public static final long TARGET_WORDS = 90000000;

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(outputPath);
        }

        public final Locale sourceLanguage;
        public final Locale targetLanguage;
        public final File[] inputRoots;
        public final File outputRoot;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = Locale.forLanguageTag(cli.getOptionValue('s'));
            targetLanguage = Locale.forLanguageTag(cli.getOptionValue('t'));

            String[] roots = cli.getOptionValues("input");
            inputRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                inputRoots[i] = new File(roots[i]);

            outputRoot = new File(cli.getOptionValue("output"));
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);

        List<MultilingualCorpus> corpora = new ArrayList<>();
        Corpora.list(null, true, corpora, args.sourceLanguage, args.targetLanguage, args.inputRoots);

        if (corpora.isEmpty())
            throw new ParseException("Input path does not contains valid bilingual data");

        File filteredFolder = new File(args.outputRoot, "filtered");
        File reducedFolder = new File(args.outputRoot, "reduced");

        LanguagePair language = new LanguagePair(args.sourceLanguage, args.targetLanguage);

        corpora = filter(language, corpora, filteredFolder);
        reduce(language, corpora, reducedFolder);
    }

    private static List<MultilingualCorpus> filter(LanguagePair language, Collection<MultilingualCorpus> corpora, File outputFolder) throws Throwable {
        if (outputFolder.exists())
            FileUtils.forceDelete(outputFolder);
        FileUtils.forceMkdir(outputFolder);

        LanguageIndex languageIndex = new LanguageIndex(language);
        ArrayList<Future<MultilingualCorpus>> futures = new ArrayList<>(corpora.size());

        ExecutorService executor = Executors.newFixedThreadPool(10);

        try {
            for (MultilingualCorpus corpus : corpora) {
                Future<MultilingualCorpus> future = executor.submit(() -> {
                    FilteredMultilingualCorpus filteredCorpus = new FilteredMultilingualCorpus(new MultilingualCorpusMask(languageIndex, corpus));
                    filteredCorpus.addNormalizer(new ControlCharsStripper());
                    filteredCorpus.addNormalizer(new XMLStripper());
                    filteredCorpus.addFilter(new EmptyLinesFilter());
                    filteredCorpus.addFilter(new RareNgramFilter());
                    filteredCorpus.addFilter(new DraftFilter());
                    filteredCorpus.addFilter(new SentenceLengthFilter());

                    ParallelFileCorpus output = new ParallelFileCorpus(outputFolder, corpus.getName(), language);

                    IOCorporaUtils.copy(filteredCorpus, output);

                    return output;
                });

                futures.add(future);
            }

            ArrayList<MultilingualCorpus> outputCorpora = new ArrayList<>(corpora.size());
            for (Future<MultilingualCorpus> future : futures) {
                outputCorpora.add(future.get());
            }

            return outputCorpora;
        } finally {
            executor.shutdownNow();
        }
    }

    private static void reduce(LanguagePair language, List<MultilingualCorpus> corpora, File outputFolder) throws Throwable {
        if (outputFolder.exists())
            FileUtils.forceDelete(outputFolder);
        FileUtils.forceMkdir(outputFolder);

        ArrayList<MultilingualCorpus> inCorpora = new ArrayList<>();

        Counts global = new Counts();
        for (MultilingualCorpus corpus : corpora) {
            Counts counts = count(corpus);

            if (counts.lines >= 100) {
                global.lines += counts.lines;
                global.words += counts.words;

                inCorpora.add(corpus);
            }
        }

        double reduction = 1.;

        if (global.words > TARGET_WORDS) {
            reduction = ((double) TARGET_WORDS) / global.words;
            System.out.println(reduction);
        }

        for (MultilingualCorpus corpus : inCorpora) {
            ParallelFileCorpus output = new ParallelFileCorpus(outputFolder, corpus.getName(), language);
            copy(corpus, output, reduction);
        }
    }

    private static Counts count(MultilingualCorpus corpus) throws IOException {
        Counts counts = new Counts();
        MultilingualCorpus.MultilingualLineReader reader = null;

        try {
            reader = corpus.getContentReader();

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                counts.lines++;
                counts.words += WordCounter.count(pair.source, pair.language.source);
            }

            return counts;
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private static final class Counts {

        public int lines = 0;
        public long words = 0;

    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination, double threshold) throws IOException {
        Random random = new Random(source.getName().hashCode());

        MultilingualCorpus.MultilingualLineReader reader = null;
        MultilingualCorpus.MultilingualLineWriter writer = null;

        try {
            reader = source.getContentReader();
            writer = destination.getContentWriter(false);

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                if (random.nextDouble() < threshold)
                    writer.write(pair);
            }
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

}
