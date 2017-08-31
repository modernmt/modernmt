package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class TranslationMemoryMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option index = Option.builder("m").longOpt("model").hasArg().required().build();
            Option corpora = Option.builder("c").longOpt("corpora").hasArgs().required().build();
            Option source = Option.builder("s").longOpt("source").hasArg().required().build();
            Option target = Option.builder("t").longOpt("target").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(index);
            cliOptions.addOption(corpora);
            cliOptions.addOption(source);
            cliOptions.addOption(target);
        }

        public final File modelPath;
        public final File[] corporaRoots;
        public final Locale sourceLanguage;
        public final Locale targetLanguage;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = Locale.forLanguageTag(cli.getOptionValue('s'));
            targetLanguage = Locale.forLanguageTag(cli.getOptionValue('t'));
            modelPath = new File(cli.getOptionValue('m'));

            String[] roots = cli.getOptionValues('c');
            corporaRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                corporaRoots[i] = new File(roots[i]);
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);

        ArrayList<MultilingualCorpus> corpora = new ArrayList<>();
        Corpora.list(null, false, corpora, args.sourceLanguage, args.targetLanguage, args.corporaRoots);

        HashMap<Domain, MultilingualCorpus> domain2corpus = new HashMap<>();
        for (MultilingualCorpus corpus : corpora) {
            long id = Long.parseLong(corpus.getName());

            Domain domain = new Domain(id);
            domain2corpus.put(domain, corpus);
        }

        LanguagePair direction = new LanguagePair(args.sourceLanguage, args.targetLanguage);
        LanguageIndex languages = new LanguageIndex(Collections.singleton(direction));

        LuceneTranslationMemory memory = null;
        try {
            memory = new LuceneTranslationMemory(languages, args.modelPath, 10);
            memory.add(domain2corpus);
        } finally {
            IOUtils.closeQuietly(memory);
        }
    }

}
