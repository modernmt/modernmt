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
import eu.modernmt.training.MultilingualCorpusMask;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AlignerThresholdMain {

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
        public final File[] inputRoots;
        public final File model;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = Language.fromString(cli.getOptionValue('s'));
            targetLanguage = Language.fromString(cli.getOptionValue('t'));

            String[] roots = cli.getOptionValues("input");
            inputRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                inputRoots[i] = new File(roots[i]);

            model = new File(cli.getOptionValue("model"));
        }

    }

    private static class Counter {

        public int value = 0;

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);

        ArrayList<MultilingualCorpus> bilingualCorpora = new ArrayList<>();
        Corpora.list(null, true, bilingualCorpora, args.sourceLanguage, args.targetLanguage, args.inputRoots);

        if (bilingualCorpora.isEmpty())
            throw new ParseException("Input path does not contains valid bilingual data");

        LanguagePair language = new LanguagePair(args.sourceLanguage, args.targetLanguage);

        FastAlign aligner = new FastAlign(args.model);
        Preprocessor preprocessor = new Preprocessor();

        HashMap<Float, Counter> counts = new HashMap<>();

        try {
            for (MultilingualCorpus _corpus : bilingualCorpora) {
                MultilingualCorpus corpus = new MultilingualCorpusMask(language, _corpus);
                process(language, preprocessor, aligner, corpus, counts);
            }
        } finally {
            IOUtils.closeQuietly(preprocessor);
        }

        for (Map.Entry<Float, Counter> entry : counts.entrySet())
            System.out.println(entry.getKey() + "\t" + entry.getValue().value);
    }

    private static void process(LanguagePair language, Preprocessor preprocessor, FastAlign aligner,
                                MultilingualCorpus corpus, HashMap<Float, Counter> counts) throws IOException {
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

                counts.computeIfAbsent(score, (key) -> new Counter()).value++;
            }
        } catch (ProcessingException | AlignerException e) {
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

}
