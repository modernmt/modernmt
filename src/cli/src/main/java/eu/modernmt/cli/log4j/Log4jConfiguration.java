package eu.modernmt.cli.log4j;

import eu.modernmt.logging.NativeLogger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

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
        setup(verbosity, null);
    }

    public static void setup(int verbosity, File logsFolder) throws IOException {
        if (verbosity < 0 || verbosity >= VERBOSITY_LEVELS.length)
            throw new IllegalArgumentException("Invalid verbosity value: " + verbosity);

        setup(VERBOSITY_LEVELS[verbosity], logsFolder);
    }

    public static void setup(Level level) throws IOException {
        setup(level, null);
    }

    public static void setup(Level level, File logsFolder) throws IOException {
        String config = loadConfig(level, logsFolder);

        File file = File.createTempFile("mmt_log4j2", "xml");
        file.deleteOnExit();

        FileUtils.write(file, config, false);

        System.setProperty("log4j.configurationFile", file.getAbsolutePath());

        NativeLogger.initialize();
    }

    private static String loadConfig(Level level, File logsFolder) throws IOException {
        String template;
        InputStream templateStream = null;

        try {
            templateStream = Log4jConfiguration.class.getResourceAsStream(
                    logsFolder == null ? LOG4J_CONSOLE_CONFIG : LOG4J_FILE_CONFIG);
            template = IOUtils.toString(templateStream);
        } finally {
            IOUtils.closeQuietly(templateStream);
        }

        template = template.replace("%level", level.name());
        if (logsFolder != null)
            template = template.replace("%root", logsFolder.getAbsolutePath());

        return template;
    }

}
