package eu.modernmt.rest.serializers;

import com.google.gson.*;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;

import java.lang.reflect.Type;

/**
 * Created by davide on 30/12/15.
 */
public class ContextVectorSerializer implements JsonSerializer<ContextVector> {

    @Override
    public JsonElement serialize(ContextVector src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();

        for (ContextVector.Entry e : src) {
            JsonObject je = new JsonObject();
            je.add("domain", context.serialize(e.domain, Domain.class));
            je.addProperty("score", e.score);

            array.add(je);
        }

        return array;
    }

}
