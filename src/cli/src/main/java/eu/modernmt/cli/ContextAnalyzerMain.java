package eu.modernmt.cli;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.lucene.LuceneAnalyzer;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
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
            Option domainMapFile = Option.builder("d").longOpt("domain-map").hasArg().required().build();
            Option corpora = Option.builder("c").longOpt("corpora").hasArgs().required().build();
            Option language = Option.builder("l").longOpt("lang").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(index);
            cliOptions.addOption(domainMapFile);
            cliOptions.addOption(corpora);
            cliOptions.addOption(language);
        }

        public final File indexPath;
        public final File domainMap;
        public final File[] corporaRoots;
        public final Locale language;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            language = Locale.forLanguageTag(cli.getOptionValue('l'));
            indexPath = new File(cli.getOptionValue('i'));
            domainMap = new File(cli.getOptionValue('d'));

            String[] roots = cli.getOptionValues('c');
            corporaRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                corporaRoots[i] = new File(roots[i]);
        }

    }

    private static HashMap<Integer, String> loadDomainMap(File file) throws IOException {
        HashMap<Integer, String> map = new HashMap<>();

        for (String line : FileUtils.readLines(file)) {
            String[] parts = line.split("\t", 2);
            map.put(Integer.parseUnsignedInt(parts[0]), parts[1]);
        }

        return map;
    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        HashMap<Integer, String> domainsMap = loadDomainMap(args.domainMap);

        HashMap<Domain, Corpus> corpora = new HashMap<>();
        for (Corpus corpus : Corpora.list(args.language, args.corporaRoots)) {
            int id = Integer.parseUnsignedInt(corpus.getName());
            String name = domainsMap.get(id);

            if (name == null)
                throw new NullPointerException("Id " + id + " has no name in domain map");

            Domain domain = new Domain(id, name);
            corpora.put(domain, corpus);
        }

        ContextAnalyzer contextAnalyzer = null;
        try {
            contextAnalyzer = new LuceneAnalyzer(args.indexPath, args.language);
            contextAnalyzer.add(corpora);
        } finally {
            IOUtils.closeQuietly(contextAnalyzer);
        }
    }

}
