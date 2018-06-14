package eu.modernmt.decoder.neural.natv;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderUnavailableException;
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class NativeProcessImpl implements NativeProcess {

    private static final Logger logger = LogManager.getLogger(NativeProcessImpl.class);

    public static class Builder implements NativeProcess.Builder {

        private final File pythonExec;
        private final File model;
        private final String[] extraArgs;

        public Builder(File pythonExec, File model) {
            this(pythonExec, null, model);
        }

        public Builder(File pythonExec, String[] extraArgs, File model) {
            this.pythonExec = pythonExec;
            this.model = model;
            this.extraArgs = extraArgs;
        }

        @Override
        public NativeProcessImpl startOnCPU() throws IOException {
            return start(-1);
        }

        @Override
        public NativeProcessImpl startOnGPU(int gpu) throws IOException {
            return start(gpu);
        }

        private NativeProcessImpl start(int gpu) throws IOException {
            ArrayList<String> command = new ArrayList<>(5);
            command.add("python");
            command.add(pythonExec.getAbsolutePath());
            command.add(model.getAbsolutePath());

            if (extraArgs != null && extraArgs.length > 0)
                command.addAll(Arrays.asList(extraArgs));

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

            if (logger.isDebugEnabled())
                logger.debug("Starting process from \"" + pythonExec + "\": " + StringUtils.join(command, ' '));

            return new NativeProcessImpl(builder.start(), gpu);
        }

    }

    private static final JsonParser parser = new JsonParser();

    private final int gpu;
    private final Process decoder;
    private final OutputStream stdin;
    private final StdoutThread stdoutThread;
    private final LogThread logThread;

    NativeProcessImpl(Process decoder, int gpu) throws IOException {
        this.gpu = gpu;
        this.decoder = decoder;
        this.stdin = decoder.getOutputStream();
        this.stdoutThread = new StdoutThread(decoder.getInputStream());
        this.logThread = new LogThread(decoder.getErrorStream());

        this.stdoutThread.start();
        this.logThread.start();

        boolean success = false;

        try {
            String line = this.stdoutThread.readLine();

            if (line == null || !line.trim().equals("ok"))
                throw new IOException("Failed to start neural decoder (cause): " + line);

            success = true;
        } finally {
            if (!success)
                close();
        }
    }

    @Override
    public int getGPU() {
        return gpu;
    }

    @Override
    public boolean isAlive() {
        return decoder.isAlive();
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence sentence, int nBest) throws DecoderException {
        return translate(direction, sentence, null, nBest);
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws DecoderException {
        if (!decoder.isAlive())
            throw new DecoderUnavailableException("Neural decoder process not available");

        String payload = serialize(direction, sentence, suggestions, nBest);

        try {
            this.stdin.write(payload.getBytes("UTF-8"));
            this.stdin.write('\n');
            this.stdin.flush();
        } catch (IOException e) {
            this.close();
            throw new DecoderException("Failed to send request to decoder process", e);
        }

        String line;
        try {
            line = stdoutThread.readLine(30, TimeUnit.SECONDS);
        } catch (IOException e) {
            this.close();
            throw new DecoderException("Failed to read response from decoder process", e);
        }

        if (line == null) {
            this.close();
            throw new DecoderException("No response from NMT process, request was '" + payload + "'");
        }

        return deserialize(sentence, line, nBest > 0);
    }

    private String serialize(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) {
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

    private Translation deserialize(Sentence sentence, String response, boolean includeNBest) throws DecoderException {
        JsonObject json;
        try {
            json = parser.parse(response).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            this.close();
            throw new DecoderException("Invalid response from NMT decoder: " + response, e);
        }

        if (json.has("error")) {
            JsonObject jsonError = json.getAsJsonObject("error");
            String type = jsonError.get("type").getAsString();
            String message = null;

            if (jsonError.has("message"))
                message = jsonError.get("message").getAsString();

            if (message == null)
                throw new DecoderException(type);
            else
                throw new DecoderException(type + " - " + message);
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
