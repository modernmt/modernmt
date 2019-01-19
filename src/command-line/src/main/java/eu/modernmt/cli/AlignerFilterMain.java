package eu.modernmt.cli;

import eu.modernmt.aligner.fastalign.FastAlign;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.training.LazyWriterMultilingualCorpus;
import eu.modernmt.training.MultilingualCorpusMask;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 18/09/17.
 */
public class AlignerFilterMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option model = Option.builder().longOpt("model").hasArg().required().build();
            Option input = Option.builder().longOpt("input").hasArg().required().build();
            Option output = Option.builder().longOpt("output").hasArg().required().build();
            Option threshold = Option.builder().longOpt("threshold").hasArg().required().build();
            Option trash = Option.builder().longOpt("trash").hasArg().required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(model);
            cliOptions.addOption(input);
            cliOptions.addOption(output);
            cliOptions.addOption(threshold);
            cliOptions.addOption(trash);
        }

        public final LanguagePair language;
        public final File model;
        public final File input;
        public final File output;
        public final float threshold;
        public final File trash;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            Language sourceLang = Language.fromString(cli.getOptionValue("s"));
            Language targetLang = Language.fromString(cli.getOptionValue("t"));
            language = new LanguagePair(sourceLang, targetLang);
            model = new File(cli.getOptionValue("model"));
            input = new File(cli.getOptionValue("input"));
            output = new File(cli.getOptionValue("output"));
            threshold = Float.parseFloat(cli.getOptionValue("threshold"));
            trash = cli.hasOption("trash") ? new File(cli.getOptionValue("trash")) : null;
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        List<MultilingualCorpus> corpora = Corpora.list(args.language, args.input);
        if (corpora.isEmpty())
            throw new ParseException("Input path does not contains valid bilingual data");

        FileUtils.deleteQuietly(args.output);
        FileUtils.forceMkdir(args.output);

        if (args.trash != null) {
            FileUtils.deleteQuietly(args.trash);
            FileUtils.forceMkdir(args.trash);
        }

        FastAlign aligner = new FastAlign(args.model);
        Preprocessor preprocessor = new Preprocessor();

        try {
            for (MultilingualCorpus corpus : corpora) {
                MultilingualCorpus trashCorpus = null;
                if (args.trash != null)
                    trashCorpus = new LazyWriterMultilingualCorpus(Corpora.rename(corpus, args.trash));

                MultilingualCorpus outCorpus = new LazyWriterMultilingualCorpus(Corpora.rename(corpus, args.output));
                corpus = new MultilingualCorpusMask(args.language, corpus);

                filter(aligner, preprocessor, args.language, corpus, outCorpus, args.threshold, trashCorpus);
            }
        } finally {
            IOUtils.closeQuietly(aligner);
            IOUtils.closeQuietly(preprocessor);
        }
    }

    private static void filter(FastAlign aligner, Preprocessor preprocessor, LanguagePair language,
                               MultilingualCorpus input, MultilingualCorpus output, float threshold, MultilingualCorpus trashCorpus) throws Throwable {
        MultilingualCorpus.MultilingualLineReader reader = null;
        MultilingualCorpus.MultilingualLineWriter writer = null;
        MultilingualCorpus.MultilingualLineWriter trashWriter = null;

        try {
            reader = input.getContentReader();
            writer = output.getContentWriter(false);
            if (trashCorpus != null)
                trashWriter = trashCorpus.getContentWriter(false);

            Batch batch = new Batch();
            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                if (!batch.add(pair)) {
                    process(aligner, preprocessor, language, threshold, batch, writer, trashWriter);
                    batch.clear();
                }
            }

            if (batch.size() > 0) {
                process(aligner, preprocessor, language, threshold, batch, writer, trashWriter);
                batch.clear();
            }
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(trashWriter);
        }
    }

    private static void process(FastAlign aligner, Preprocessor preprocessor, LanguagePair language, float threshold,
                                Batch batch, MultilingualCorpus.MultilingualLineWriter writer, MultilingualCorpus.MultilingualLineWriter trashWriter) throws Throwable {
        List<String> sources = batch.getSources();
        List<String> targets = batch.getTargets();

        List<Sentence> tokSources = preprocessor.process(language, sources);
        List<Sentence> tokTargets = preprocessor.process(language.reversed(), targets);
        Alignment[] alignments = aligner.getAlignments(language, tokSources, tokTargets);

        MultilingualCorpus.StringPair pair = new MultilingualCorpus.StringPair(language, null, null);

        for (int i = 0; i < alignments.length; i++) {
            if (alignments[i].getScore() >= threshold) {
                pair.source = sources.get(i);
                pair.target = targets.get(i);
                writer.write(pair);
            } else if (trashWriter != null) {
                pair.source = sources.get(i);
                pair.target = targets.get(i);
                trashWriter.write(pair);
            }
        }

        batch.clear();
    }

    private static final class Batch {

        private final int maxSize;
        private final ArrayList<String> sources;
        private final ArrayList<String> targets;

        public Batch() {
            this(Runtime.getRuntime().availableProcessors() * 20);
        }

        public Batch(int size) {
            this.maxSize = size;
            this.sources = new ArrayList<>(size);
            this.targets = new ArrayList<>(size);
        }

        public boolean add(MultilingualCorpus.StringPair pair) {
            sources.add(pair.source);
            targets.add(pair.target);

            return sources.size() < maxSize;
        }

        public void clear() {
            sources.clear();
            targets.clear();
        }

        public int size() {
            return sources.size();
        }

        public ArrayList<String> getSources() {
            return sources;
        }

        public ArrayList<String> getTargets() {
            return targets;
        }

    }

}
