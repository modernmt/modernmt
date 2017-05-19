package eu.modernmt.rest.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.io.TokensOutputStream;

import java.lang.reflect.Type;

/**
 * Created by davide on 17/12/15.
 */
public class TranslationHypothesisSerializer implements JsonSerializer<TranslationHypothesis> {

    @Override
    public JsonElement serialize(TranslationHypothesis src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("translation", TokensOutputStream.toString(src, false, true));
        json.addProperty("totalScore", src.getTotalScore());
        json.add("scores", context.serialize(src.getScores()));

        return json;
    }

}
