package eu.modernmt.decoder.neural.queue;

import com.google.gson.*;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class PythonDecoderImpl extends PythonProcess implements PythonDecoder {

    public static class Builder implements PythonDecoder.Builder {

        private final String pythonExec;
        private final File pythonModule;
        private final String main;
        private final File model;
        private final String[] extraArgs;

        public Builder(String pythonExec, File pythonModule, File model) {
            this(pythonExec, pythonModule, null, null, model);
        }

        public Builder(File pythonModule, File model) {
            this("python3", pythonModule, null, null, model);
        }

        public Builder(File pythonModule, String[] extraArgs, File model) {
            this("python3", pythonModule, null, extraArgs, model);
        }

        public Builder(String pythonExec, File pythonModule, String main, String[] extraArgs, File model) {
            this.pythonExec = pythonExec;
            this.pythonModule = pythonModule;
            this.main = main;
            this.model = model;
            this.extraArgs = extraArgs;
        }

        @Override
        public PythonDecoder startOnCPU() throws IOException {
            return start(-1);
        }

        @Override
        public PythonDecoder startOnGPU(int gpu) throws IOException {
            return start(gpu);
        }

        private PythonDecoderImpl start(int gpu) throws IOException {
            ArrayList<String> command = new ArrayList<>(6);
            command.add(pythonExec);
            command.add(pythonModule.getAbsolutePath());

            if (main != null)
                command.add(main);

            command.add(model.getAbsolutePath());

            if (extraArgs != null && extraArgs.length > 0)
                command.addAll(Arrays.asList(extraArgs));

            String logLevel = PythonProcess.getNativeLogLevel();
            if (logLevel != null) {
                command.add("--log-level");
                command.add(logLevel);
            }

            if (gpu >= 0) {
                command.add("--gpu");
                command.add(Integer.toString(gpu));
            }

            ProcessBuilder builder = new ProcessBuilder(command);
            PythonDecoderImpl process = new PythonDecoderImpl(builder.start(), gpu);
            boolean success = false;

            try {
                process.connect();
                process.init();
                success = true;

                return process;
            } finally {
                if (!success)
                    IOUtils.closeQuietly(process);
            }
        }

    }

    private static final JsonParser parser = new JsonParser();

    private final int gpu;
    private boolean alive;

    protected PythonDecoderImpl(Process process) {
        this(process, -1);
    }

    protected PythonDecoderImpl(Process process, int gpu) {
        super(process);
        this.gpu = gpu;
    }

    protected void init() throws IOException {
        String line = super.recv();
        if (!"READY".equals(line))
            throw new IOException("Failed to start neural decoder, received: " + line);

        this.alive = true;
    }

    @Override
    public int getGPU() {
        return gpu;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void test() throws DecoderException {
        translate(null, "{}");
    }

    @Override
    public Translation translate(LanguageDirection direction, Sentence sentence, int nBest) throws DecoderException {
        return this.translate(direction, new Sentence[]{sentence}, nBest)[0];
    }

    @Override
    public Translation translate(LanguageDirection direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws DecoderException {
        return this.translate(direction, new Sentence[]{sentence}, suggestions, nBest)[0];
    }

    @Override
    public Translation[] translate(LanguageDirection direction, Sentence[] sentences, int nBest) throws DecoderException {
        return this.translate(sentences, serialize(direction, sentences, null, null));
    }

    @Override
    public Translation[] translate(LanguageDirection direction, Sentence[] sentences, ScoreEntry[] suggestions, int nBest) throws DecoderException {
        return this.translate(sentences, serialize(direction, sentences, suggestions, null));
    }

    @Override
    public Translation align(LanguageDirection direction, Sentence sentence, String[] translation) throws DecoderException {
        Sentence[] sentences = new Sentence[]{sentence};
        String[][] translations = new String[][]{translation};
        return this.translate(sentences, serialize(direction, new Sentence[]{sentence}, null, translations))[0];
    }

    @Override
    public Translation[] align(LanguageDirection direction, Sentence[] sentences, String[][] translations) throws DecoderException {
        return this.translate(sentences, serialize(direction, sentences, null, translations));
    }

    private synchronized Translation[] translate(Sentence[] sentences, String payload) throws DecoderException {
        if (!isAlive())
            throw new DecoderUnavailableException("Neural decoder process not available");

        boolean success = false;

        try {
            super.send(payload);

            String response = super.recv(30, TimeUnit.SECONDS);
            if (response == null)
                throw new DecoderUnavailableException("Neural decoder process not responding (timeout)");

            Translation[] translations = deserialize(response, sentences);

            success = true;
            return translations;
        } catch (IOException e) {
            throw new DecoderUnavailableException("Failed to send request to decoder process", e);
        } finally {
            if (!success) {
                this.alive = false;
                this.close();
            }
        }
    }

    private String serialize(LanguageDirection direction, Sentence[] sentences, ScoreEntry[] suggestions, String[][] forcedTranslations) {
        String[] serialized = new String[sentences.length];
        for (int i = 0; i < serialized.length; i++)
            serialized[i] = TokensOutputStream.serialize(sentences[i], false, true);
        String text = StringUtils.join(serialized, '\n');

        JsonObject json = new JsonObject();
        json.addProperty("q", text);
        json.addProperty("sl", direction.source.toLanguageTag());
        json.addProperty("tl", direction.target.toLanguageTag());

        if (forcedTranslations != null) {
            String[] serializedForcedTranslations = new String[forcedTranslations.length];
            for (int i = 0; i < serializedForcedTranslations.length; i++)
                serializedForcedTranslations[i] = StringUtils.join(forcedTranslations[i], ' ');
            json.addProperty("f", StringUtils.join(serializedForcedTranslations, '\n'));
        }

        if (suggestions != null && suggestions.length > 0) {
            JsonArray array = new JsonArray();

            for (ScoreEntry entry : suggestions) {
                JsonObject obj = new JsonObject();
                obj.addProperty("sl", entry.language.source.toLanguageTag());
                obj.addProperty("tl", entry.language.target.toLanguageTag());
                obj.addProperty("seg", StringUtils.join(entry.sentenceTokens, ' '));
                obj.addProperty("tra", StringUtils.join(entry.translationTokens, ' '));
                obj.addProperty("scr", entry.score);

                array.add(obj);
            }

            json.add("hints", array);
        }

        return json.toString().replace('\n', ' ');
    }

    private Translation[] deserialize(String response, Sentence[] sentences) throws IOException, DecoderException {
        JsonObject json;
        try {
            json = parser.parse(response).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new IOException("Invalid response from NMT decoder: " + response, e);
        }

        boolean success = json.get("success").getAsBoolean();

        if (success) {
            JsonArray data = json.getAsJsonArray("data");

            Translation[] translations = new Translation[data.size()];
            for (int i = 0; i < translations.length; i++) {
                JsonObject e = data.get(i).getAsJsonObject();

                Word[] words = TokensOutputStream.deserializeWords(e.get("text").getAsString());
                JsonElement jsonAlignment = e.get("a");
                Alignment alignment = jsonAlignment == null ? null : parseAlignment(jsonAlignment.getAsJsonArray());

                translations[i] = new Translation(words, sentences[i], alignment);
            }

            return translations;
        } else {
            String type = json.get("type").getAsString();
            String message = null;

            if (json.has("msg"))
                message = json.get("msg").getAsString();

            throw (message == null) ? new DecoderException(type) : new DecoderException(type + " - " + message);
        }
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

}
