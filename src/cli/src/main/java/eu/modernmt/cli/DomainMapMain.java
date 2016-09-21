package eu.modernmt.cli;

import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.persistence.Connection;
import eu.modernmt.persistence.DomainDAO;
import eu.modernmt.persistence.sqlite.SQLiteDatabase;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.File;
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
            Option language = Option.builder("l").longOpt("lang").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(dbPath);
            cliOptions.addOption(corpora);
            cliOptions.addOption(language);
        }

        public final File dbPath;
        public final File[] corporaRoots;
        public final Locale language;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            language = Locale.forLanguageTag(cli.getOptionValue('l'));
            dbPath = new File(cli.getOptionValue("db"));

            String[] roots = cli.getOptionValues('c');
            corporaRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                corporaRoots[i] = new File(roots[i]);
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        SQLiteDatabase db = new SQLiteDatabase("jdbc:sqlite:" + args.dbPath, null, null);

        Connection connection = null;
        try {
            connection = db.getConnection();
            db.drop(connection);
            db.create(connection);

            DomainDAO dao = db.getDomainDAO(connection);

            for (Corpus corpus : Corpora.list(args.language, args.corporaRoots)) {
                Domain domain = new Domain(0, corpus.getName());
                domain = dao.put(domain);

                System.out.println(domain.getId() + "\t" + domain.getName());
            }
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }
}
