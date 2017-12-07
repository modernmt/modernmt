package eu.modernmt.decoder.neural.execution;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import eu.modernmt.decoder.neural.NeuralDecoderException;
import eu.modernmt.decoder.neural.NeuralDecoderRejectedExecutionException;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 05/06/17.
 * <p>
 * A NativeProcess represents a separate process that is launched by an MMT engine to run an NeuralDecoder.
 * The NativeProcess object is thus to run and request translations to its specific decoder process.
 * If necessary, it also handles its close.
 */
class NativeProcess implements Closeable {

    private static final Logger logger = LogManager.getLogger(NativeProcess.class);

    /**
     * A NativeProcess.Builder is an object that allows the creation of a NativeProcess
     * (and thus the launch of the separate process for the NMT decoder).
     * It offers methods for running the decoder process either on a CPU or on a GPU.
     */
    public static class Builder {

        private final File home;
        private final File model;

        public Builder(File home, File model) {
            this.home = home;
            this.model = model;
        }

        /**
         * This methods launches a separate process for an NeuralDecoder running on an available CPU
         * and returns a NativeProcess that allows to interact with such process.
         *
         * @return a NativeProcess object, referencing the launched process
         * @throws IOException            if the communication with the decoder process raised unexpected issues
         * @throws NeuralDecoderException if the decoder process itself raised unexpected issues
         */
        public NativeProcess startOnCPU() throws IOException, NeuralDecoderException {
            return start(-1);
        }

        /**
         * This methods launches a separate process for an NeuralDecoder running on a CPU
         * and returns a NativeProcess that allows to interact with such process.
         *
         * @return a NativeProcess object, referencing the launched process
         * @throws IOException            if the communication with the decoder process raised unexpected issues
         * @throws NeuralDecoderException if the decoder process itself raised unexpected issues
         */
        public NativeProcess startOnGPU(int gpu) throws IOException, NeuralDecoderException {
            return start(gpu);
        }

        private NativeProcess start(int gpu) throws IOException, NeuralDecoderException {
            ArrayList<String> command = new ArrayList<>(5);
            command.add("python");
            command.add("main_loop.py");
            command.add(model.getAbsolutePath());

            String logLevel = LogThread.getNativeLogLevel();
            if (logLevel != null) {
                command.add("--log-level");
                command.add(logLevel);
            }

            if (gpu >= 0) {
                command.add("--gpu");
                command.add(Integer.toString(gpu));
            }

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(home);

            if (logger.isDebugEnabled())
                logger.debug("Starting process from \"" + home + "\": " + StringUtils.join(command, ' '));

            return new NativeProcess(builder.start());
        }

    }

    private static final JsonParser parser = new JsonParser();

    private final Process decoder;          // the decoder Python process
    private final OutputStream stdin;       // stream to the standard input that the decoder process will read
    private final BufferedReader stdout;    // reader to the standard output that the decoder process will write
    private final LogThread logThread;      // separate thread for logging

    /**
     * Create a new NativeProcess that connects to a specific NeuralDecoder process.
     * After it is created, the NativeProcess allows communication with the decoder.
     * NOTE: The process must be running already.
     *
     * @param decoder an already running decoder process
     * @throws IOException
     * @throws NeuralDecoderException
     */
    NativeProcess(Process decoder) throws IOException, NeuralDecoderException {
        this.decoder = decoder;
        this.stdin = decoder.getOutputStream();
        this.stdout = new BufferedReader(new InputStreamReader(decoder.getInputStream()));
        this.logThread = new LogThread(decoder.getErrorStream());

        this.logThread.start();

        /*Wait for feedback from the engine: it can be either "ok" or an exception. */
        try {
            String line = this.stdout.readLine();
            if (line == null || !line.trim().equals("ok"))
                deserialize(null, line, false);
        } catch (IOException | NeuralDecoderException e) {
            IOUtils.closeQuietly(this.stdin);
            IOUtils.closeQuietly(this.stdout);
            throw e;
        }

    }

    /**
     * This method requests a translation to this decoder process.
     *
     * @param direction the direction of the translation to execute
     * @param sentence  the source sentence to translate
     * @param nBest     number of hypothesis to return (default 0)
     * @return the translation of the passed sentence
     * @throws NeuralDecoderException
     */
    public Translation translate(LanguagePair direction, Sentence sentence, int nBest) throws NeuralDecoderException {
        return translate(direction, sentence, null, nBest);
    }

    /**
     * This method requests a translation to this decoder process.
     *
     * @param direction   the direction of the translation to execute
     * @param sentence    the source sentence to translate
     * @param suggestions an array of translation suggestions that the decoder will study before the translation
     * @param nBest       number of hypothesis to return (default 0)
     * @return the translation of the passed sentence
     * @throws NeuralDecoderException
     */
    public Translation translate(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws NeuralDecoderException {
        if (!decoder.isAlive())
            throw new NeuralDecoderRejectedExecutionException();

        String payload = serialize(direction, sentence, suggestions, nBest);

        try {
            this.stdin.write(payload.getBytes("UTF-8"));
            this.stdin.write('\n');
            this.stdin.flush();
        } catch (IOException e) {
            throw new NeuralDecoderException("Failed to send request to NMT decoder", e);
        }

        String line;
        try {
            line = stdout.readLine();
        } catch (IOException e) {
            throw new NeuralDecoderException("Failed to read response from NMT decoder", e);
        }

        if (line == null)
            throw new NeuralDecoderException("No response from NMT process, request was '" + payload + "'");

        return deserialize(sentence, line, nBest > 0);
    }

    private static String serialize(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) {
        String text = TokensOutputStream.serialize(sentence, false, true);

        JsonObject json = new JsonObject();
        json.addProperty("source", text);
        json.addProperty("source_language", direction.source.toLanguageTag());
        json.addProperty("target_language", direction.target.toLanguageTag());

        if (nBest > 0)
            json.addProperty("n_best", nBest);

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

    private static Translation deserialize(Sentence sentence, String response, boolean includeNBest) throws NeuralDecoderException {
        JsonObject json;
        try {
            json = parser.parse(response).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new NeuralDecoderException("Invalid response from NMT decoder: " + response, e);
        }

        if (json.has("error")) {
            JsonObject jsonError = json.getAsJsonObject("error");
            String type = jsonError.get("type").getAsString();
            String message = null;

            if (jsonError.has("message"))
                message = jsonError.get("message").getAsString();

            throw NeuralDecoderException.fromPythonError(type, message);
        }

        JsonArray jsonArray = json.getAsJsonArray("result");

        if (logger.isDebugEnabled())
            logger.debug("Received translations: " + jsonArray);

        ArrayList<Translation> translations = new ArrayList<>(jsonArray.size());

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonTranslation = jsonArray.get(i).getAsJsonObject();

            Word[] text = explodeText(jsonTranslation.get("text").getAsString());
            Alignment alignment = parseAlignment(jsonTranslation.get("alignment").getAsJsonArray());

            translations.add(new Translation(text, sentence, alignment));
        }

        if (translations.isEmpty())
            return Translation.emptyTranslation(sentence);

        Translation best = translations.get(0);
        Translation result;

        if (includeNBest) {
            result = new Translation(best.getWords(), sentence, best.getWordAlignment());
            result.setNbest(translations);
        } else {
            result = best;
        }

        return result;
    }

    private static Word[] explodeText(String text) {
        if (text.isEmpty())
            return new Word[0];

        String[] pieces = TokensOutputStream.deserialize(text);
        Word[] words = new Word[pieces.length];

        for (int i = 0; i < pieces.length; i++) {
            String rightSpace = i < pieces.length - 1 ? " " : null;
            words[i] = new Word(pieces[i], rightSpace);
        }

        return words;
    }

    private static Alignment parseAlignment(JsonArray array) {
        if (array.size() == 0)
            return new Alignment(new int[0], new int[0]);

        JsonArray sourceIndexesArray = array.get(0).getAsJsonArray();
        JsonArray targetIndexesArray = array.get(1).getAsJsonArray();

        int size = sourceIndexesArray.size();
        int[] sourceIndexes = new int[size];
        int[] targetIndexes = new int[size];

        for (int i = 0; i < size; i++) {
            sourceIndexes[i] = sourceIndexesArray.get(i).getAsInt();
            targetIndexes[i] = targetIndexesArray.get(i).getAsInt();
        }

        return new Alignment(sourceIndexes, targetIndexes);
    }

    /**
     * This method kills this decoder process.
     * <p>
     * It first tries to gently kill it.
     * If after 5 seconds the process is still alive, it is forcibly destroyed.
     */
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

        public static String getNativeLogLevel() {
            Level level = logger.getLevel();

            for (Map.Entry<String, Level> entry : LOG_LEVELS.entrySet()) {
                if (level.equals(entry.getValue()))
                    return entry.getKey().toLowerCase();
            }

            return null;
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
                logger.info("Closing log thread for NMT process");
            }
        }

    }

}
