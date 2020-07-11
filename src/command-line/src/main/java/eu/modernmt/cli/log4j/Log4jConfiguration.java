package eu.modernmt.cli.log4j;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by davide on 02/03/16.
 */
public class Log4jConfiguration {

    private static final String LOG4J_CONSOLE_CONFIG = "/eu/modernmt/cli/log4j/log4j-console.xml";
    private static final String LOG4J_FILE_CONFIG = "/eu/modernmt/cli/log4j/log4j-file.xml";

    private static final Level[] VERBOSITY_LEVELS = new Level[]{
            Level.ERROR, Level.INFO, Level.DEBUG, Level.ALL
    };

    public static void setup(int verbosity) throws IOException {
        setup(null, verbosity);
    }

    public static void setup(File logFile, int verbosity) throws IOException {
        if (verbosity < 0 || verbosity >= VERBOSITY_LEVELS.length)
            throw new IllegalArgumentException("Invalid verbosity value: " + verbosity);

        setup(logFile, VERBOSITY_LEVELS[verbosity]);
    }

    public static void setup(Level level) throws IOException {
        setup(null, level);
    }

    public static void setup(File logFile, Level level) throws IOException {
        String config = loadConfig(level, logFile);

        File configFile = File.createTempFile("mmt_log4j2", "xml");

        try {
            FileUtils.write(configFile, config, false);
            Configurator.initialize("mmt-log4j", configFile.getAbsolutePath());
        } finally {
            FileUtils.deleteQuietly(configFile);
        }
    }

    private static String loadConfig(Level level, File logFile) throws IOException {
        String template;
        InputStream templateStream = null;

        try {
            templateStream = Log4jConfiguration.class.getResourceAsStream(
                    logFile == null ? LOG4J_CONSOLE_CONFIG : LOG4J_FILE_CONFIG);
            template = IOUtils.toString(templateStream);
        } finally {
            IOUtils.closeQuietly(templateStream);
        }

        template = template.replace("%{level}", level.name());
        if (logFile != null)
            template = template.replace("%{log_file}", logFile.getAbsolutePath());

        return template;
    }

}
