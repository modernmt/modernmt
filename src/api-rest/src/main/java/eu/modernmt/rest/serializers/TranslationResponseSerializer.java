package eu.modernmt.rest.serializers;

import com.google.gson.*;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.decoder.HasFeatureScores;
import eu.modernmt.model.Alignment;
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

        JsonObject json = new JsonObject();
        json.addProperty("decodingTime", src.translation.getElapsedTime());
        json.addProperty("translation", src.translation.toString());

        if (src.verbose) {
            json.add("translationTokens", serializeTokens(src.translation));
            json.add("sentenceTokens", serializeTokens(source));
            json.add("alignment", context.serialize(src.translation.getAlignment(), Alignment.class));
        }

        if (src.translation.hasNbest()) {
            JsonArray array = new JsonArray();
            for (Translation hypothesis : src.translation.getNbest())
                array.add(serializeHypothesis(context, hypothesis, src.verbose));
            json.add("nbest", array);
        }

        if (src.context != null)
            json.add("contextVector", context.serialize(src.context, ContextVector.class));

        return json;
    }

    private static JsonElement serializeHypothesis(JsonSerializationContext context, Translation translation, boolean verbose) {
        JsonObject json = new JsonObject();
        json.addProperty("translation", translation.toString());

        if (verbose) {
            json.add("translationTokens", serializeTokens(translation));
            json.add("alignment", context.serialize(translation.getAlignment(), Alignment.class));

            if (translation instanceof HasFeatureScores) {
                HasFeatureScores src = (HasFeatureScores) translation;
                json.addProperty("totalScore", src.getTotalScore());
                json.add("scores", serializeScores(context, src.getScores()));
            }
        }

        return json;

    }

    private static JsonElement serializeScores(JsonSerializationContext context, Map<DecoderFeature, float[]> scores) {
        JsonObject json = new JsonObject();

        for (Map.Entry<DecoderFeature, float[]> entry : scores.entrySet()) {
            String name = entry.getKey().getName();
            JsonElement value = context.serialize(entry.getValue());

            json.add(name, value);
        }

        return json;
    }

    private static JsonArray serializeTokens(Sentence sentence) {
        JsonArray array = new JsonArray();
        //TODO: must implement
        return array;
    }

}
