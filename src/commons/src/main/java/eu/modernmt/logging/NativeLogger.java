package eu.modernmt.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by davide on 27/12/16.
 */
public class NativeLogger {

    static {
        System.loadLibrary("mmt_logging");
    }

    private static final String NAME_PREFIX = "native.";
    private static final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();
    private static final Level[] LEVELS = new Level[]{
            Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL, Level.TRACE
    };

    public static native void initialize();

    protected static int createLogger(String _loggerName) {
        final String loggerName = NAME_PREFIX + _loggerName;

        Logger logger = loggers.computeIfAbsent(loggerName, name -> LogManager.getLogger(loggerName));
        Level level = logger.getLevel();

        for (int i = 0; i < LEVELS.length; i++) {
            if (LEVELS[i] == level)
                return i + 1;
        }

        return 1; // TRACE
    }

    protected static void log(String loggerName, int intLevel, String message) {
        loggerName = NAME_PREFIX + loggerName;

        Logger logger = loggers.get(loggerName);

        if (logger == null)
            return;

        intLevel = intLevel - 1;
        Level level = (0 <= intLevel && intLevel < LEVELS.length) ? LEVELS[intLevel] : Level.TRACE;
        logger.log(level, message);
    }
}
