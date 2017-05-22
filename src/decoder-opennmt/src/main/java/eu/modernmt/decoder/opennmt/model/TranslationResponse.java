package eu.modernmt.decoder.opennmt.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;

/**
 * Created by davide on 22/05/17.
 */
public class TranslationResponse {

    private static final JsonParser parser = new JsonParser();

    public static TranslationResponse fromJSON(String jsonString) {
        JsonObject json = parser.parse(jsonString).getAsJsonObject();

        long id = json.get("id").getAsLong();
        OpenNMTException exception = null;
        Word[] translation = null;

        if (json.has("error")) {
            JsonObject jsonError = json.getAsJsonObject("error");
            String type = jsonError.get("type").getAsString();
            String message = null;

            if (jsonError.has("message"))
                message = jsonError.get("message").getAsString();

            exception = OpenNMTException.fromPythonError(type, message);
        } else {
            String text = json.get("translation").getAsString();
            translation = explode(text);
        }

        return new TranslationResponse(id, exception, translation);
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

    private long id;
    private OpenNMTException exception;
    private Word[] translation;

    public TranslationResponse(long id, OpenNMTException exception, Word[] translation) {
        this.id = id;
        this.exception = exception;
        this.translation = translation;
    }

    public long getId() {
        return id;
    }

    public boolean hasException() {
        return exception != null;
    }

    public OpenNMTException getException() {
        return exception;
    }

    public DecoderTranslation getTranslation(Sentence source) {
        return new DecoderTranslation(translation, source, null);
    }
}
