package eu.modernmt.decoder.opennmt.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.modernmt.decoder.opennmt.storage.Suggestion;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.Sentence;

import java.util.List;

/**
 * Created by davide on 22/05/17.
 */
public class TranslationRequest {

    private long id;
    private final Sentence sentence;
    private List<Suggestion> suggestions;

    public TranslationRequest(Sentence sentence) {
        this.sentence = sentence;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public Sentence getSentence() {
        return sentence;
    }

    public String toJSON() {
        String text = TokensOutputStream.toString(sentence, false, true);

        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("source", text);

        if (suggestions != null && !suggestions.isEmpty()) {
            JsonArray array = new JsonArray();

            for (Suggestion suggestion : suggestions) {
                JsonObject obj = new JsonObject();
                obj.addProperty("source", TokensOutputStream.toString(suggestion.source, false, true));
                obj.addProperty("target", TokensOutputStream.toString(suggestion.translation, false, true));
                obj.addProperty("score", suggestion.score);

                array.add(obj);
            }

            json.add("suggestions", array);
        }

        return json.toString().replace('\n', ' ');
    }
}
