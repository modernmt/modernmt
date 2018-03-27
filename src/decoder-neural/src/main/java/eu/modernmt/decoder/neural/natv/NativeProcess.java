package eu.modernmt.decoder.neural.natv;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import eu.modernmt.decoder.neural.NeuralDecoderException;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 05/06/17.
 * <p>
 * A NativeProcess represents a separate process that is launched by an MMT engine to run an NeuralDecoder.
 * The NativeProcess object is thus to run and request translations to its specific decoder process.
 * If necessary, it also handles its close.
 */
public class NativeProcess implements Closeable {

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

            return new NativeProcess(builder.start(), gpu);
        }

    }

    private static final JsonParser parser = new JsonParser();

    private final int gpu;
    private final Process decoder;
    private final OutputStream stdin;
    private final StdoutThread stdoutThread;
    private final LogThread logThread;

    NativeProcess(Process decoder, int gpu) throws NeuralDecoderException {
        this.gpu = gpu;
        this.decoder = decoder;
        this.stdin = decoder.getOutputStream();
        this.stdoutThread = new StdoutThread(decoder.getInputStream());
        this.logThread = new LogThread(decoder.getErrorStream());

        this.stdoutThread.start();
        this.logThread.start();

        try {
            String line;

            try {
                line = this.stdoutThread.readLine();
            } catch (IOException e) {
                throw new NeuralDecoderException("Failed to start neural decoder", e);
            }

            if (line == null || !line.trim().equals("ok"))
                throw new NeuralDecoderException("Failed to start neural decoder (cause): " + line);
        } catch (NeuralDecoderException e) {
            close();
            throw e;
        }

    }

    public int getGPU() {
        return gpu;
    }

    public boolean isAlive() {
        return decoder.isAlive();
    }

    public Translation translate(LanguagePair direction, String variant, Sentence sentence, int nBest) throws NeuralDecoderException {
        return translate(direction, variant, sentence, null, nBest);
    }

    public Translation translate(LanguagePair direction, String variant, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws NeuralDecoderException {
        if (!decoder.isAlive())
            throw new NeuralDecoderException("Neural decoder process not available");

        String payload = serialize(direction, variant, sentence, suggestions, nBest);

        try {
            this.stdin.write(payload.getBytes("UTF-8"));
            this.stdin.write('\n');
            this.stdin.flush();
        } catch (IOException e) {
            this.close();
            throw new NeuralDecoderException("Failed to send request to decoder process", e);
        }

        String line;
        try {
            line = stdoutThread.readLine(30, TimeUnit.SECONDS);
        } catch (IOException e) {
            this.close();
            throw new NeuralDecoderException("Failed to read response from decoder process", e);
        }

        if (line == null) {
            this.close();
            throw new NeuralDecoderException("No response from NMT process, request was '" + payload + "'");
        }

        return deserialize(sentence, line, nBest > 0);
    }

    private String serialize(LanguagePair direction, String variant, Sentence sentence, ScoreEntry[] suggestions, int nBest) {
        String text = TokensOutputStream.serialize(sentence, false, true);

        JsonObject json = new JsonObject();
        json.addProperty("source", text);
        json.addProperty("source_language", direction.source.toLanguageTag());
        json.addProperty("target_language", direction.target.toLanguageTag());

        if (variant != null && !variant.isEmpty())
            json.addProperty("variant", variant);

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

    private Translation deserialize(Sentence sentence, String response, boolean includeNBest) throws NeuralDecoderException {
        JsonObject json;
        try {
            json = parser.parse(response).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            this.close();
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

            Word[] text = TokensOutputStream.deserializeWords(jsonTranslation.get("text").getAsString());
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

    @Override
    public void close() {
        logThread.interrupt();
        stdoutThread.interrupt();

        IOUtils.closeQuietly(stdin);

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
    }

}
