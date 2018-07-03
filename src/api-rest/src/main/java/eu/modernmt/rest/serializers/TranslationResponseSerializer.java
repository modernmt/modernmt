package eu.modernmt.rest.serializers;

import com.google.gson.*;
import eu.modernmt.model.*;
import eu.modernmt.rest.model.TranslationResponse;

import java.lang.reflect.Type;

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
        json.addProperty("sourceWordCount", source.getWords().length);
        json.addProperty("targetWordCount", src.translation.getWords().length);

        if (src.verbose) {
            json.add("translationTokens", serializeTokens(src.translation));
            json.add("sentenceTokens", serializeTokens(source));
            json.add("alignment", context.serialize(src.translation.getSentenceAlignment(), Alignment.class));
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
            json.add("alignment", context.serialize(translation.getSentenceAlignment(), Alignment.class));
        }

        return json;
    }

    private static JsonArray serializeTokens(Sentence sentence) {
        JsonArray array = new JsonArray();
        for (Token token : sentence)
            array.add(token.toString());
        return array;
    }

}
