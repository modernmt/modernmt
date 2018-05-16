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

public class AlignerScoreMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option model = Option.builder().longOpt("model").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(model);
        }

        public final Language sourceLanguage;
        public final Language targetLanguage;
        public final File inputRoot;
        public final File model;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = Language.fromString(cli.getOptionValue('s'));
            targetLanguage = Language.fromString(cli.getOptionValue('t'));
            inputRoot = new File(cli.getOptionValue("input"));
            model = new File(cli.getOptionValue("model"));
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);

        ArrayList<MultilingualCorpus> bilingualCorpora = new ArrayList<>();
        Corpora.list(null, true, bilingualCorpora, args.sourceLanguage, args.targetLanguage, args.inputRoot);

        if (bilingualCorpora.isEmpty())
            throw new ParseException("Input path does not contains valid bilingual data");

        FastAlign aligner = new FastAlign(args.model);
        Preprocessor preprocessor = new Preprocessor();

        try {
            ThresholdCalculator calculator = new ThresholdCalculator(args.sourceLanguage, args.targetLanguage, preprocessor, aligner, args.inputRoot);
            float threshold = calculator.calculate();

            System.out.println(Float.toString(threshold));
        } finally {
            IOUtils.closeQuietly(preprocessor);
            IOUtils.closeQuietly(aligner);
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
