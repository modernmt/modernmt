package eu.modernmt.cli;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.aligner.fastalign.FastAlign;
import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.training.LazyWriterMultilingualCorpus;
import eu.modernmt.training.MultilingualCorpusMask;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class AlignerFilterMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option goldPath = Option.builder().longOpt("gold").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();
            Option model = Option.builder().longOpt("model").hasArg().required().build();
            Option verbose = Option.builder().longOpt("verbose").hasArg().required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(goldPath);
            cliOptions.addOption(outputPath);
            cliOptions.addOption(model);
            cliOptions.addOption(verbose);
        }

        public final Language sourceLanguage;
        public final Language targetLanguage;
        public final File[] inputRoots;
        public final File goldRoot;
        public final File outputRoot;
        public final File model;
        public final boolean verbose;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = Language.fromString(cli.getOptionValue('s'));
            targetLanguage = Language.fromString(cli.getOptionValue('t'));

            String[] roots = cli.getOptionValues("input");
            inputRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                inputRoots[i] = new File(roots[i]);

            goldRoot = new File(cli.getOptionValue("gold"));
            outputRoot = new File(cli.getOptionValue("output"));
            model = new File(cli.getOptionValue("model"));
            verbose = cli.hasOption("verbose");
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);

        ArrayList<MultilingualCorpus> bilingualCorpora = new ArrayList<>();
        Corpora.list(null, true, bilingualCorpora, args.sourceLanguage, args.targetLanguage, args.inputRoots);

        if (bilingualCorpora.isEmpty())
            throw new ParseException("Input path does not contains valid bilingual data");

        FileUtils.deleteDirectory(args.outputRoot);
        FileUtils.forceMkdir(args.outputRoot);

        LanguagePair language = new LanguagePair(args.sourceLanguage, args.targetLanguage);

        FastAlign aligner = new FastAlign(args.model);
        Preprocessor preprocessor = new Preprocessor();

        try {
            ThresholdCalculator calculator = new ThresholdCalculator(args.sourceLanguage, args.targetLanguage, preprocessor, aligner, args.goldRoot);
            float threshold = calculator.calculate();

            System.err.println("Filtering with threshold: " + threshold);

            Filter filter = new Filter(language, aligner, preprocessor);

            for (MultilingualCorpus _corpus : bilingualCorpora) {
                MultilingualCorpus corpus = new MultilingualCorpusMask(language, _corpus);
                MultilingualCorpus output = new LazyWriterMultilingualCorpus(Corpora.rename(corpus, args.outputRoot));

                filter.apply(corpus, output, threshold, args.verbose);
            }
        } finally {
            IOUtils.closeQuietly(preprocessor);
            IOUtils.closeQuietly(aligner);
        }
    }

    private static final class Filter {

        private final LanguagePair language;
        private final FastAlign aligner;
        private final Preprocessor preprocessor;

        public Filter(LanguagePair language, FastAlign aligner, Preprocessor preprocessor) {
            this.language = language;
            this.aligner = aligner;
            this.preprocessor = preprocessor;
        }

        public void apply(MultilingualCorpus corpus, MultilingualCorpus output, float threshold, boolean verbose) throws IOException {
            Batch batch = new Batch();

            MultilingualCorpus.MultilingualLineReader reader = null;
            MultilingualCorpus.MultilingualLineWriter writer = null;

            try {
                reader = corpus.getContentReader();
                writer = output.getContentWriter(false);

                MultilingualCorpus.StringPair pair;
                while ((pair = reader.read()) != null) {
                    while (!batch.add(pair)) {
                        processBatch(batch, threshold, writer, verbose);
                        batch.clear();
                    }
                }

                if (batch.size() > 0) {
                    processBatch(batch, threshold, writer, verbose);
                    batch.clear();
                }
            } catch (ProcessingException | AlignerException e) {
                throw new IOException(e);
            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(writer);
            }
        }

        private void processBatch(Batch batch, float threshold, MultilingualCorpus.MultilingualLineWriter writer, boolean verbose)
                throws ProcessingException, AlignerException, IOException {
            List<Sentence> sources = preprocessor.process(language, batch.getSources());
            List<Sentence> targets = preprocessor.process(language.reversed(), batch.getTargets());
            Alignment[] alignments = aligner.getAlignments(language, sources, targets);

            int i = 0;
            for (MultilingualCorpus.StringPair pair : batch.getPairs()) {
                if (alignments[i].getScore() >= threshold) {
                    writer.write(pair);
                } else {
                    if (verbose)
                        System.err.println(pair);
                }

                i++;
            }
        }

    }

    private static final class Batch {

        private final int maxSize;
        private final ArrayList<String> sources;
        private final ArrayList<String> targets;
        private final ArrayList<MultilingualCorpus.StringPair> pairs;

        public Batch() {
            this(Runtime.getRuntime().availableProcessors() * 2);
        }

        public Batch(int size) {
            this.maxSize = size;
            this.sources = new ArrayList<>(size);
            this.targets = new ArrayList<>(size);
            this.pairs = new ArrayList<>(size);
        }

        public boolean add(MultilingualCorpus.StringPair pair) {
            if (sources.size() >= maxSize)
                return false;

            sources.add(pair.source);
            targets.add(pair.target);
            pairs.add(pair);

            return true;
        }

        public void clear() {
            sources.clear();
            targets.clear();
            pairs.clear();
        }

        public int size() {
            return sources.size();
        }

        public ArrayList<MultilingualCorpus.StringPair> getPairs() {
            return pairs;
        }

        public ArrayList<String> getSources() {
            return sources;
        }

        public ArrayList<String> getTargets() {
            return targets;
        }

    }

    private static class Counter implements Comparable<Counter> {

        public final float threshold;
        public int count = 0;

        public Counter(float threshold) {
            this.threshold = threshold;
        }

        @Override
        public int compareTo(@NotNull Counter o) {
            return Float.compare(threshold, o.threshold);
        }
    }

    private static final class ThresholdCalculator {

        private final LanguagePair language;
        private final List<MultilingualCorpus> goldCorpora;
        private final Preprocessor preprocessor;
        private final FastAlign aligner;

        public ThresholdCalculator(Language source, Language target, Preprocessor preprocessor, FastAlign aligner, File path) throws IOException {
            this.language = new LanguagePair(source, target);
            this.preprocessor = preprocessor;
            this.aligner = aligner;

            this.goldCorpora = new ArrayList<>();
            Corpora.list(null, true, this.goldCorpora, source, target, path);
        }

        public float calculate() throws IOException {
            HashMap<Float, Counter> counts = new HashMap<>();

            for (MultilingualCorpus _corpus : goldCorpora) {
                MultilingualCorpus corpus = new MultilingualCorpusMask(language, _corpus);
                process(corpus, counts);
            }

            // Select threshold for 99%
            ArrayList<Counter> array = new ArrayList<>(counts.values());
            Collections.sort(array);
            Collections.reverse(array);

            double total = 0;
            for (Counter c : array)
                total += c.count;

            double partial = 0;
            for (Counter c : array) {
                partial += c.count;
                if (partial / total > 0.99)
                    return c.threshold;
            }

            return array.get(array.size() - 1).threshold;
        }

        private void process(MultilingualCorpus corpus, HashMap<Float, Counter> counts) throws IOException {
            MultilingualCorpus.MultilingualLineReader reader = null;

            try {
                reader = corpus.getContentReader();

                MultilingualCorpus.StringPair pair;
                while ((pair = reader.read()) != null) {
                    Sentence source = preprocessor.process(language, pair.source);
                    Sentence target = preprocessor.process(language.reversed(), pair.target);
                    Alignment alignment = aligner.getAlignment(language, source, target);

                    float score = alignment.getScore();
                    score = ((int) (score * 2 + .5f)) / 2.f;

                    counts.computeIfAbsent(score, Counter::new).count++;
                }
            } catch (ProcessingException | AlignerException e) {
                throw new IOException(e);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }

    }
}
