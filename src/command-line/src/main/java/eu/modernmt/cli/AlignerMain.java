package eu.modernmt.cli;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.aligner.fastalign.FastAlign;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by davide on 18/09/17.
 */
public class AlignerMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option model = Option.builder().longOpt("model").hasArg().required().build();
            Option input = Option.builder().longOpt("source").hasArg().required().build();
            Option translations = Option.builder().longOpt("target").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(model);
            cliOptions.addOption(input);
            cliOptions.addOption(translations);
        }

        public final LanguagePair language;
        public final File model;
        public final File source;
        public final File target;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            Locale sourceLang = Locale.forLanguageTag(cli.getOptionValue("s"));
            Locale targetLang = Locale.forLanguageTag(cli.getOptionValue("t"));
            language = new LanguagePair(sourceLang, targetLang);
            model = new File(cli.getOptionValue("model"));
            source = new File(cli.getOptionValue("source"));
            target = new File(cli.getOptionValue("target"));
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        FastAlign aligner = new FastAlign(args.model);

        ParallelFileCorpus corpus = new ParallelFileCorpus(null, args.language, args.source, args.target);
        MultilingualCorpus.MultilingualLineReader reader = null;

        Batch batch = new Batch();

        try {
            reader = corpus.getContentReader();

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                while (!batch.add(pair)) {
                    processBatch(args.language, batch, aligner);
                }
            }

            if (batch.size() > 0)
                processBatch(args.language, batch, aligner);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private static void processBatch(LanguagePair language, Batch batch, Aligner aligner) throws AlignerException {
        Alignment[] alignments = aligner.getAlignments(language, batch.getSources(), batch.getTargets());

        for (Alignment alignment : alignments)
            System.out.println(alignment);

        batch.clear();
    }

    private static final class Batch {

        private final int maxSize;
        private final ArrayList<Sentence> sources;
        private final ArrayList<Sentence> targets;

        public Batch() {
            this(Runtime.getRuntime().availableProcessors() * 2);
        }

        public Batch(int size) {
            this.maxSize = size;
            this.sources = new ArrayList<>(size);
            this.targets = new ArrayList<>(size);
        }

        public boolean add(MultilingualCorpus.StringPair pair) {
            if (sources.size() >= maxSize)
                return false;

            sources.add(toSentence(pair.source));
            targets.add(toSentence(pair.target));

            return true;
        }

        public void clear() {
            sources.clear();
            targets.clear();
        }

        public int size() {
            return sources.size();
        }

        public ArrayList<Sentence> getSources() {
            return sources;
        }

        public ArrayList<Sentence> getTargets() {
            return targets;
        }

        private static Sentence toSentence(String line) {
            String[] tokens = line.split("\\s+");
            Word[] words = new Word[tokens.length];

            for (int i = 0; i < tokens.length; i++)
                words[i] = new Word(tokens[i], " ");

            return new Sentence(words);
        }

    }

}
