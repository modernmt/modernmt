package eu.modernmt.cli;

import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.persistence.Connection;
import eu.modernmt.persistence.DomainDAO;
import eu.modernmt.persistence.cassandra.CassandraDatabase;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by davide on 21/09/16.
 */
public class DomainMapMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option dbPath = Option.builder().longOpt("db").hasArg().required().build();
            Option corpora = Option.builder("c").longOpt("corpora").hasArgs().required().build();
            Option source = Option.builder("s").longOpt("source").hasArg().required().build();
            Option target = Option.builder("t").longOpt("target").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(dbPath);
            cliOptions.addOption(corpora);
            cliOptions.addOption(source);
            cliOptions.addOption(target);
        }

        public final File dbPath;
        public final File[] corporaRoots;
        public final Locale sourceLanguage;
        public final Locale targetLanguage;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = Locale.forLanguageTag(cli.getOptionValue('s'));
            targetLanguage = Locale.forLanguageTag(cli.getOptionValue('t'));

            dbPath = new File(cli.getOptionValue("db"));

            String[] roots = cli.getOptionValues('c');
            corporaRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                corporaRoots[i] = new File(roots[i]);
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        // TODO: USE LOCALHOST AND PORT FOR REAL CASSANDRA CLUSTER
        CassandraDatabase db = new CassandraDatabase("localhost", 9042);

        Connection connection = null;
        try {
            db.drop();
            db.create();

            connection = db.getConnection();
            DomainDAO dao = db.getDomainDAO(connection);

            ArrayList<BilingualCorpus> corpora = new ArrayList<>();
            Corpora.list(null, false, corpora, args.sourceLanguage, args.targetLanguage, args.corporaRoots);

            for (BilingualCorpus corpus : corpora) {
                Domain domain = new Domain(0, corpus.getName());
                domain = dao.put(domain);

                System.out.println(domain.getId() + "\t" + domain.getName());
            }
        } finally {
            IOUtils.closeQuietly(connection);
            IOUtils.closeQuietly(db);
        }
    }
}
