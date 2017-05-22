package eu.modernmt.decoder.opennmt.model;

import com.google.gson.JsonObject;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.Sentence;

/**
 * Created by davide on 22/05/17.
 */
public class TranslationRequest {

    private long id;
    private final Sentence sentence;

    public TranslationRequest(Sentence sentence) {
        this.sentence = sentence;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Sentence getSentence() {
        return sentence;
    }

    public String toJSON() {
        String text = TokensOutputStream.toString(sentence, false, true);

        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("source", text);

        return json.toString().replace('\n', ' ');
    }
}
