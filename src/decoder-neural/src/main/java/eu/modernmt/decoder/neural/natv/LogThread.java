package eu.modernmt.decoder.neural.natv;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

class LogThread extends StreamPollingThread {

    private static final JsonParser parser = new JsonParser();
    private static final Logger logger = LogManager.getLogger(NativeProcess.class);
    private static final HashMap<String, Level> LOG_LEVELS = new HashMap<>(5);

    static {
        LOG_LEVELS.put("CRITICAL", Level.FATAL);
        LOG_LEVELS.put("ERROR", Level.ERROR);
        LOG_LEVELS.put("WARNING", Level.WARN);
        LOG_LEVELS.put("INFO", Level.INFO);
        LOG_LEVELS.put("DEBUG", Level.DEBUG);
    }

    public static String getNativeLogLevel() {
        Level level = logger.getLevel();

        for (Map.Entry<String, Level> entry : LOG_LEVELS.entrySet()) {
            if (level.equals(entry.getValue()))
                return entry.getKey().toLowerCase();
        }

        return null;
    }

    public LogThread(InputStream stdout) {
        super(stdout);
    }

    @Override
    protected void onLineRead(String line) {
        if (line == null)
            return;

        try {
            JsonObject json = parser.parse(line).getAsJsonObject();

            String strLevel = json.get("level").getAsString();
            String message = json.get("message").getAsString();
            String loggerName = json.get("logger").getAsString();

            Level level = LOG_LEVELS.getOrDefault(strLevel, Level.DEBUG);
            logger.log(level, "(" + loggerName + ") " + message);
        } catch (JsonSyntaxException e) {
            logger.warn("Unable to parse python log entry: " + line);
        }
    }

    @Override
    protected void onIOException(IOException e) {
        logger.error("Failed to read from neural decoder STDERR", e);
    }

}
