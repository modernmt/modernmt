package eu.modernmt.cli;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.aligner.fastalign.FastAlign;
import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageIndex;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AlignerFilterMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();
            Option model = Option.builder().longOpt("model").hasArg().required().build();
            Option threshold = Option.builder().longOpt("threshold").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(outputPath);
            cliOptions.addOption(model);
            cliOptions.addOption(threshold);
        }

        public final Language sourceLanguage;
        public final Language targetLanguage;
        public final File[] inputRoots;
        public final File outputRoot;
        public final File model;
        public final float threshold;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = Language.fromString(cli.getOptionValue('s'));
            targetLanguage = Language.fromString(cli.getOptionValue('t'));

            String[] roots = cli.getOptionValues("input");
            inputRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                inputRoots[i] = new File(roots[i]);

            outputRoot = new File(cli.getOptionValue("output"));
            model = new File(cli.getOptionValue("model"));
            threshold = Float.parseFloat(cli.getOptionValue("threshold"));
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
            Filter filter = new Filter(language, aligner, preprocessor);

            for (MultilingualCorpus _corpus : bilingualCorpora) {
                MultilingualCorpus corpus = new MultilingualCorpusMask(language, _corpus);
                MultilingualCorpus output = new LazyWriterMultilingualCorpus(Corpora.rename(corpus, args.outputRoot));

                filter.apply(corpus, output, args.threshold);
            }
        } finally {
            IOUtils.closeQuietly(preprocessor);
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

        public void apply(MultilingualCorpus corpus, MultilingualCorpus output, float threshold) throws IOException {
            Batch batch = new Batch();

            MultilingualCorpus.MultilingualLineReader reader = null;
            MultilingualCorpus.MultilingualLineWriter writer = null;

            try {
                reader = corpus.getContentReader();
                writer = output.getContentWriter(false);

                MultilingualCorpus.StringPair pair;
                while ((pair = reader.read()) != null) {
                    while (!batch.add(pair)) {
                        processBatch(batch, threshold, writer);
                        batch.clear();
                    }
                }

                if (batch.size() > 0) {
                    processBatch(batch, threshold, writer);
                    batch.clear();
                }
            } catch (ProcessingException | AlignerException e) {
                throw new IOException(e);
            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(writer);
            }
        }

        private void processBatch(Batch batch, float threshold, MultilingualCorpus.MultilingualLineWriter writer)
                throws ProcessingException, AlignerException, IOException {
            List<Sentence> sources = preprocessor.process(language, batch.getSources());
            List<Sentence> targets = preprocessor.process(language.reversed(), batch.getTargets());
            Alignment[] alignments = aligner.getAlignments(language, sources, targets);

            int i = 0;
            for (MultilingualCorpus.StringPair pair : batch.getPairs()) {
                if (alignments[i].getScore() > threshold) {
                    writer.write(pair);
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

}
