package eu.modernmt.api.serializers;

import com.google.gson.*;
import eu.modernmt.api.model.TranslationResponse;
import eu.modernmt.model.*;

import java.lang.reflect.Type;

/**
 * Created by davide on 30/12/15.
 */
public class TranslationResponseSerializer implements JsonSerializer<TranslationResponse> {

    @Override
    public JsonElement serialize(TranslationResponse src, Type typeOfSrc, JsonSerializationContext context) {
        Sentence source = src.translation.getSource();

        JsonObject json = new JsonObject();
        json.addProperty("translation", src.translation.toString());
        json.addProperty("sourceWordCount", source.getWords().length);
        json.addProperty("targetWordCount", src.translation.getWords().length);

        if (src.verbose) {
            json.add("translationTokens", serializeTokens(src.translation));
            json.add("sentenceTokens", serializeTokens(source));
            json.add("alignment", context.serialize(src.translation.getSentenceAlignment(), Alignment.class));
        }

        if (src.translation.hasAlternatives()) {
            JsonArray array = new JsonArray();
            for (Translation alternative : src.translation.getAlternatives())
                array.add(serializeHypothesis(context, alternative,  src.verbose));

            json.add("alternatives", array);
        }

        if (src.context != null)
            json.add("contextVector", context.serialize(src.context, ContextVector.class));

        json.addProperty("priority", src.priority.toString().toLowerCase());
        json.addProperty("totalTime", src.getTotalTime());
        json.addProperty("memoryLookupTime", src.translation.getMemoryLookupTime());
        json.addProperty("decodingTime", src.translation.getDecodeTime());
        json.addProperty("queueTime", src.translation.getQueueTime());
        json.addProperty("queueLength", src.translation.getQueueLength());

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
