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

    public static void setup(int verbosity) {
        setup(verbosity, null);
    }

    public static void setup(int verbosity, File logsFolder) {
        if (verbosity < 0 || verbosity >= VERBOSITY_LEVELS.length)
            throw new IllegalArgumentException("Invalid verbosity value: " + verbosity);

        setup(VERBOSITY_LEVELS[verbosity], logsFolder);
    }

    public static void setup(Level level) {
        setup(level, null);
    }

    public static void setup(Level level, File logsFolder) {
        String template = loadTemplate(level, logsFolder);
        if (template == null)
            return;

//        ConfigurationSource source;
        try {
            File file = File.createTempFile("mmt_log4j2", "xml");
            FileUtils.write(file, template, false);

            System.setProperty("log4j.configurationFile", file.getAbsolutePath());

//            source = new ConfigurationSource(new StringInputStream(template));
        } catch (IOException e) {
            throw new Error(e); // Impossible
        }

//        Configurator.initialize(null, source);
        NativeLogger.initialize();
    }

    private static String loadTemplate(Level level, File logsFolder) {
        String template = null;
        InputStream templateStream = null;

        try {
            templateStream = Log4jConfiguration.class.getResourceAsStream(
                    logsFolder == null ? LOG4J_CONSOLE_CONFIG : LOG4J_FILE_CONFIG);
            template = IOUtils.toString(templateStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(templateStream);
        }

        if (template == null)
            return null;

        template = template.replace("%level", level.name());
        if (logsFolder != null)
            template = template.replace("%root", logsFolder.getAbsolutePath());

        return template;
    }

}
