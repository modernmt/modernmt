package eu.modernmt.cli;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.lucene.LuceneAnalyzer;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class ContextAnalyzerMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option index = Option.builder("i").longOpt("index-path").hasArg().required().build();
            Option corpora = Option.builder("c").longOpt("corpora").hasArgs().required().build();
            Option language = Option.builder("l").longOpt("lang").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(index);
            cliOptions.addOption(corpora);
            cliOptions.addOption(language);
        }

        public final File indexPath;
        public final File[] corporaRoots;
        public final Locale language;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            language = Locale.forLanguageTag(cli.getOptionValue('l'));
            indexPath = new File(cli.getOptionValue('i'));

            String[] roots = cli.getOptionValues('c');
            corporaRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                corporaRoots[i] = new File(roots[i]);
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        HashMap<Domain, Corpus> corpora = new HashMap<>();
        for (Corpus corpus : Corpora.list(args.language, args.corporaRoots)) {
            // TODO: we're making the assumption that domain names ARE numbers, this is unacceptable for a real case scenario
            Domain domain = new Domain(Integer.parseUnsignedInt(corpus.getName()), corpus.getName());
            corpora.put(domain, corpus);
        }

        ContextAnalyzer contextAnalyzer = new LuceneAnalyzer(args.indexPath, args.language);
        contextAnalyzer.add(corpora);
    }

}
