package eu.modernmt.decoder.opennmt.execution;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.OpenNMTRejectedExecutionException;
import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 05/06/17.
 */
class NativeProcess implements Closeable {

    private static final Logger logger = LogManager.getLogger(NativeProcess.class);

    public static class Builder {

        private final File home;
        private final File model;

        public Builder(File home, File model) {
            this.home = home;
            this.model = model;
        }

        public NativeProcess start() throws IOException {
            return start(0);
        }

        public NativeProcess start(int gpu) throws IOException {
            String[] command = new String[]{
                    "python", "nmt_decoder.py", model.getAbsolutePath(), "--gpu", Integer.toString(gpu)
            };

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(home);

            if (logger.isDebugEnabled())
                logger.debug("Starting process from \"" + home + "\": " + StringUtils.join(command, ' '));

            return new NativeProcess(builder.start());
        }

    }

    private static final JsonParser parser = new JsonParser();

    private final Process decoder;
    private final OutputStream stdin;
    private final BufferedReader stdout;
    private final LogThread logThread;

    private NativeProcess(Process decoder) {
        this.decoder = decoder;
        this.stdin = decoder.getOutputStream();
        this.stdout = new BufferedReader(new InputStreamReader(decoder.getInputStream()));
        this.logThread = new LogThread(decoder.getErrorStream());

        this.logThread.start();
    }

    public Word[] translate(Sentence sentence) throws OpenNMTException {
        return translate(sentence, null);
    }

    public Word[] translate(Sentence sentence, ScoreEntry[] suggestions) throws OpenNMTException {
        if (!decoder.isAlive())
            throw new OpenNMTRejectedExecutionException();

        String payload = serialize(sentence, suggestions);

        try {
            this.stdin.write(payload.getBytes("UTF-8"));
            this.stdin.write('\n');
            this.stdin.flush();
        } catch (IOException e) {
            throw new OpenNMTException("Failed to send request to OpenNMT decoder", e);
        }

        String line;
        try {
            line = stdout.readLine();
        } catch (IOException e) {
            throw new OpenNMTException("Failed to read response from OpenNMT decoder", e);
        }

        if (line == null)
            throw new OpenNMTException("No response from OpenNMT process, request was '" + payload + "'");

        return deserialize(line);
    }

    private static String serialize(Sentence sentence, ScoreEntry[] suggestions) {
        String text = TokensOutputStream.toString(sentence, false, true);

        JsonObject json = new JsonObject();
        json.addProperty("source", text);

        if (suggestions != null && suggestions.length > 0) {
            JsonArray array = new JsonArray();

            for (ScoreEntry entry : suggestions) {
                JsonObject obj = new JsonObject();
                obj.addProperty("source", StringUtils.join(entry.sentence, ' '));
                obj.addProperty("target", StringUtils.join(entry.translation, ' '));
                obj.addProperty("score", entry.score);

                array.add(obj);
            }

            json.add("suggestions", array);
        }

        return json.toString().replace('\n', ' ');
    }

    public static Word[] deserialize(String response) throws OpenNMTException {
        JsonObject json;
        try {
            json = parser.parse(response).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new OpenNMTException("Invalid response from OpenNMT decoder: " + response, e);
        }

        if (json.has("error")) {
            JsonObject jsonError = json.getAsJsonObject("error");
            String type = jsonError.get("type").getAsString();
            String message = null;

            if (jsonError.has("message"))
                message = jsonError.get("message").getAsString();

            throw OpenNMTException.fromPythonError(type, message);
        }

        return explode(json.get("translation").getAsString());
    }

    private static Word[] explode(String text) {
        if (text.isEmpty())
            return new Word[0];

        String[] pieces = text.split(" +");
        Word[] words = new Word[pieces.length];

        for (int i = 0; i < pieces.length; i++) {
            String rightSpace = i < pieces.length - 1 ? " " : null;

            String placeholder = TokensOutputStream.deescapeWhitespaces(pieces[i]);
            words[i] = new Word(placeholder, rightSpace);
        }

        return words;
    }

    @Override
    public void close() throws IOException {
        decoder.destroy();

        try {
            decoder.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Nothing to do
        }

        if (decoder.isAlive())
            decoder.destroyForcibly();

        try {
            decoder.waitFor();
        } catch (InterruptedException e) {
            // Nothing to do
        }

        this.logThread.interrupt();
    }

    private static class LogThread extends Thread {

        private static final HashMap<String, Level> LOG_LEVELS = new HashMap<>(5);

        static {
            LOG_LEVELS.put("CRITICAL", Level.FATAL);
            LOG_LEVELS.put("ERROR", Level.ERROR);
            LOG_LEVELS.put("WARNING", Level.WARN);
            LOG_LEVELS.put("INFO", Level.INFO);
            LOG_LEVELS.put("DEBUG", Level.DEBUG);
        }

        private final BufferedReader reader;

        private LogThread(InputStream stream) {
            this.reader = new BufferedReader(new InputStreamReader(stream));
        }

        @Override
        public void run() {
            String line;

            try {
                while ((line = reader.readLine()) != null) {
                    JsonObject json;
                    try {
                        json = parser.parse(line).getAsJsonObject();

                        String strLevel = json.get("level").getAsString();
                        String message = json.get("message").getAsString();
                        String loggerName = json.get("logger").getAsString();

                        Level level = LOG_LEVELS.getOrDefault(strLevel, Level.DEBUG);
                        logger.log(level, "(" + loggerName + ") " + message);
                    } catch (JsonSyntaxException e) {
                        logger.warn("Unable to parse python log entry: " + line);
                    }
                }
            } catch (IOException e) {
                logger.info("Closing log thread for OpenNMT process");
            }
        }

    }

}
