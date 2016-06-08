package eu.modernmt.rest.serializers;

import com.google.gson.*;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.processing.util.TokensOutputter;
import eu.modernmt.rest.model.TranslationResponse;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by davide on 30/12/15.
 */
public class TranslationResponseSerializer implements JsonSerializer<TranslationResponse> {

    @Override
    public JsonElement serialize(TranslationResponse src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("translation", src.processing ? src.translation.toString() : TokensOutputter.toString(src.translation, false, true));
        json.addProperty("decodingTime", src.translation.getElapsedTime());

        if (src.session > 0L)
            json.addProperty("session", src.session);

        List<TranslationHypothesis> nbest = src.translation.getNbest();
        if (nbest != null) {
            JsonArray array = new JsonArray();
            for (TranslationHypothesis hypothesis : nbest)
                array.add(TranslationHypothesisSerializer.serialize(hypothesis, context, src.processing));
            json.add("nbest", array);
        }

        if (src.context != null) {
            JsonArray array = new JsonArray();
            for (ContextDocument document : src.context)
                array.add(context.serialize(document));
            json.add("context", array);
        }

        return json;
    }
}
