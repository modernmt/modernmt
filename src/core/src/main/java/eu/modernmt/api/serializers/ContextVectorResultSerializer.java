package eu.modernmt.api.serializers;

import com.google.gson.*;
import eu.modernmt.api.model.ContextVectorResult;
import eu.modernmt.lang.Language2;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Memory;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Created by davide on 30/12/15.
 */
public class ContextVectorResultSerializer implements JsonSerializer<ContextVectorResult> {

    @Override
    public JsonElement serialize(ContextVectorResult src, Type typeOfSrc, JsonSerializationContext context) {
        JsonElement result;

        if (src.backwardCompatible) {

            JsonArray array = new JsonArray();

            //if backwardCompatble is true, there is only one value in the map
            // so you can get it as the first element of map.values
            ContextVector vector = src.map.values().iterator().next();

            for (ContextVector.Entry e : vector) {
                JsonObject je = new JsonObject();
                je.add("domain", context.serialize(e.memory, Memory.class));
                je.addProperty("score", e.score);
                array.add(je);
            }
            result = array;

        } else {
            JsonObject object = new JsonObject();
            object.addProperty("source", src.source.toLanguageTag());

            JsonObject jsonMap = new JsonObject();
            object.add("vectors", jsonMap);

            for (Map.Entry<Language2, ContextVector> entry : src.map.entrySet()) {
                Language2 target = entry.getKey();
                jsonMap.add(target.toLanguageTag(), serialize(entry.getValue(), context));
            }

            result = object;

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
