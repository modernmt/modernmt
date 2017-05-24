package eu.modernmt.decoder.opennmt.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.modernmt.decoder.opennmt.storage.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.Sentence;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by davide on 22/05/17.
 */
public class TranslationRequest {

    private long id;
    private final Sentence sentence;
    private ScoreEntry[] suggestions;

    public TranslationRequest(Sentence sentence) {
        this.sentence = sentence;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setSuggestions(ScoreEntry[] suggestions) {
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
}
