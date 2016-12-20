package eu.modernmt.rest.serializers;

import com.google.gson.*;
import eu.modernmt.context.ContextScore;
import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.model.Sentence;
import eu.modernmt.rest.model.TranslationResponse;

import java.lang.reflect.Type;
import java.util.List;

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

        if (src.session > 0L)
            json.addProperty("session", src.session);

        List<TranslationHypothesis> nbest = src.translation.getNbest();
        if (nbest != null) {
            JsonArray array = new JsonArray();
            for (TranslationHypothesis hypothesis : nbest)
                array.add(context.serialize(hypothesis, TranslationHypothesis.class));
            json.add("nbest", array);
        }

        if (src.context != null) {
            JsonArray array = new JsonArray();
            for (ContextScore document : src.context)
                array.add(context.serialize(document));
            json.add("context", array);
        }

        return json;
    }
}
