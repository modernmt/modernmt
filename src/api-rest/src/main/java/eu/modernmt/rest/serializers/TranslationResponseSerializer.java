package eu.modernmt.rest.serializers;

import com.google.gson.*;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.decoder.HasFeatureScores;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.rest.model.TranslationResponse;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Created by davide on 30/12/15.
 */
public class TranslationResponseSerializer implements JsonSerializer<TranslationResponse> {

    @Override
    public JsonElement serialize(TranslationResponse src, Type typeOfSrc, JsonSerializationContext context) {
        Sentence source = src.translation.getSource();

        int sourceWordCount = source.getWords().length;
        int targetWordCount = src.translation.getWords().length;

        JsonObject json = new JsonObject();
        json.addProperty("translation", src.translation.toString());
        json.addProperty("decodingTime", src.translation.getElapsedTime());
        json.addProperty("sourceWordCount", sourceWordCount);
        json.addProperty("targetWordCount", targetWordCount);

        if (src.translation.hasNbest()) {
            JsonArray array = new JsonArray();
            for (Translation hypothesis : src.translation.getNbest())
                array.add(serialize(context, hypothesis));
            json.add("nbest", array);
        }

        if (src.context != null)
            json.add("contextVector", context.serialize(src.context, ContextVector.class));

        return json;
    }

    private static JsonElement serialize(JsonSerializationContext context, Translation translation) {
        JsonObject json = new JsonObject();
        json.addProperty("translation", TokensOutputStream.toString(translation, false, true));

        if (translation instanceof HasFeatureScores) {
            HasFeatureScores src = (HasFeatureScores) translation;
            json.addProperty("totalScore", src.getTotalScore());
            json.add("scores", serialize(context, src.getScores()));
        }

        return json;

    }

    private static JsonElement serialize(JsonSerializationContext context, Map<DecoderFeature, float[]> scores) {
        JsonObject json = new JsonObject();

        for (Map.Entry<DecoderFeature, float[]> entry : scores.entrySet()) {
            String name = entry.getKey().getName();
            JsonElement value = context.serialize(entry.getValue());

            json.add(name, value);
        }

        return json;
    }

}
