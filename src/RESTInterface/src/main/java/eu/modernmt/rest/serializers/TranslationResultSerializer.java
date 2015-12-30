package eu.modernmt.rest.serializers;

import com.google.gson.*;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.rest.model.TranslationResult;

import java.lang.reflect.Type;

/**
 * Created by davide on 30/12/15.
 */
public class TranslationResultSerializer implements JsonSerializer<TranslationResult> {

    @Override
    public JsonElement serialize(TranslationResult src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("translation", src.translation);
        if (src.session > 0L)
            json.addProperty("session", src.session);
        if (src.context != null) {
            JsonArray array = new JsonArray();
            for (ContextDocument document : src.context)
                array.add(context.serialize(document));
            json.add("context", array);
        }

        return json;
    }
}
