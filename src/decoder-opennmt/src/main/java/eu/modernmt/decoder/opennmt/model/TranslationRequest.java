package eu.modernmt.decoder.opennmt.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.modernmt.decoder.opennmt.storage.TranslationsStorage;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.Sentence;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by davide on 22/05/17.
 */
public class TranslationRequest {

    private long id;
    private final Sentence sentence;
    private TranslationsStorage.SearchResult suggestions;

    public TranslationRequest(Sentence sentence) {
        this.sentence = sentence;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setSuggestions(TranslationsStorage.SearchResult suggestions) {
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

            for (int i = 0; i < suggestions.size(); i++) {
                JsonObject obj = new JsonObject();
                obj.addProperty("source", StringUtils.join(suggestions.entries[i].sentence, ' '));
                obj.addProperty("target", StringUtils.join(suggestions.entries[i].translation, ' '));
                obj.addProperty("score", suggestions.scores[i]);

                array.add(obj);
            }

            json.add("suggestions", array);
        }

        return json.toString().replace('\n', ' ');
    }
}
