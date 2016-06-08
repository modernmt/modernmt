package eu.modernmt.rest.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.processing.util.TokensOutputter;

/**
 * Created by davide on 17/12/15.
 */
public class TranslationHypothesisSerializer {

    public static JsonElement serialize(TranslationHypothesis src, JsonSerializationContext context, boolean processing) {
        JsonObject json = new JsonObject();
        json.addProperty("translation", processing ? src.toString() : TokensOutputter.toString(src, false, true));
        json.addProperty("totalScore", src.getTotalScore());
        json.add("scores", context.serialize(src.getScores()));

        return json;
    }

}
