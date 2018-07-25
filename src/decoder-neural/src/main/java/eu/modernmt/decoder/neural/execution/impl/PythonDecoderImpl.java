package eu.modernmt.decoder.neural.execution.impl;

import com.google.gson.*;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.decoder.neural.execution.PythonDecoder;
import eu.modernmt.decoder.neural.execution.PythonProcess;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguagePair;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PythonDecoderImpl extends PythonProcess implements PythonDecoder {

    public static class Builder implements PythonDecoder.Builder {

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
        public PythonDecoder startOnCPU() throws IOException {
            return start(-1);
        }

        @Override
        public PythonDecoder startOnGPU(int gpu) throws IOException {
            return start(gpu);
        }

        private PythonDecoderImpl start(int gpu) throws IOException {
            ArrayList<String> command = new ArrayList<>(5);
            command.add("python");
            command.add(pythonExec.getAbsolutePath());
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


            Map<String, String> env = builder.environment();
            env.put("CUDA_DEVICE_ORDER", "PCI_BUS_ID");  // see issue #152
            env.put("CUDA_VISIBLE_DEVICES", Integer.toString(gpu));

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
    public Translation translate(LanguagePair direction, Sentence sentence, int nBest) throws DecoderException {
        return this.translate(direction, sentence, null, nBest);
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws DecoderException {
        if (!isAlive())
            throw new DecoderUnavailableException("Neural decoder process not available");

        String payload = serialize(direction, sentence, suggestions);

        boolean success = false;

        try {
            super.send(payload);

            String response = super.recv(30, TimeUnit.SECONDS);
            if (response == null)
                throw new DecoderUnavailableException("Neural decoder process not responding (timeout)");

            Translation translation = deserialize(response, sentence);

            success = true;
            return translation;
        } catch (IOException e) {
            throw new DecoderUnavailableException("Failed to send request to decoder process", e);
        } finally {
            if (!success) {
                this.alive = false;
                this.close();
            }
        }
    }

    private String serialize(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions) {
        String text = TokensOutputStream.serialize(sentence, false, true);

        JsonObject json = new JsonObject();
        json.addProperty("q", text);
        json.addProperty("sl", direction.source.toLanguageTag());
        json.addProperty("tl", direction.target.toLanguageTag());

        if (suggestions != null && suggestions.length > 0) {
            JsonArray array = new JsonArray();

            for (ScoreEntry entry : suggestions) {
                JsonObject obj = new JsonObject();
                obj.addProperty("sl", entry.language.source.toLanguageTag());
                obj.addProperty("tl", entry.language.target.toLanguageTag());
                obj.addProperty("seg", StringUtils.join(entry.sentence, ' '));
                obj.addProperty("tra", StringUtils.join(entry.translation, ' '));
                obj.addProperty("scr", entry.score);

                array.add(obj);
            }

            json.add("hints", array);
        }

        return json.toString().replace('\n', ' ');
    }

    private Translation deserialize(String response, Sentence sentence) throws IOException, DecoderException {
        JsonObject json;
        try {
            json = parser.parse(response).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new IOException("Invalid response from NMT decoder: " + response, e);
        }

        boolean success = json.get("success").getAsBoolean();
        JsonObject data = json.getAsJsonObject("data");

        if (success) {
            Word[] words = TokensOutputStream.deserializeWords(data.get("text").getAsString());
            JsonElement jsonAlignment = data.get("a");
            Alignment alignment = jsonAlignment == null ? null : parseAlignment(jsonAlignment.getAsJsonArray());

            return new Translation(words, sentence, alignment);
        } else {
            String type = data.get("type").getAsString();
            String message = null;

            if (data.has("msg"))
                message = data.get("msg").getAsString();

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
