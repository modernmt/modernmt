package eu.modernmt.rest.serializers;

import com.google.gson.*;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Memory;
import eu.modernmt.rest.model.ContextVectorResult;

import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 30/12/15.
 */
public class ContextVectorResultSerializer implements JsonSerializer<ContextVectorResult> {

    @Override
    public JsonElement serialize(ContextVectorResult src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.addProperty("source", src.source.toLanguageTag());

        JsonObject jsonMap = new JsonObject();
        result.add("vectors", jsonMap);

        for (Map.Entry<Locale, ContextVector> entry : src.map.entrySet()) {
            Locale target = entry.getKey();
            jsonMap.add(target.toLanguageTag(), serialize(entry.getValue(), context));
        }

        return result;
    }

    private JsonElement serialize(ContextVector vector, JsonSerializationContext context) {
        JsonArray array = new JsonArray();

        for (ContextVector.Entry e : vector) {
            JsonObject je = new JsonObject();
            je.add("memory", context.serialize(e.memory, Memory.class));
            je.addProperty("score", e.score);

            array.add(je);
        }

        return array;
    }

}
