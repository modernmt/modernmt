package eu.modernmt.rest.serializers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import eu.modernmt.model.Alignment;

import java.lang.reflect.Type;

/**
 * Created by davide on 17/12/15.
 */
public class AlignmentSerializer implements JsonSerializer<Alignment> {

    @Override
    public JsonElement serialize(Alignment src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();

        for (int[] a : src) {
            JsonArray ja = new JsonArray();
            ja.add(a[0]);
            ja.add(a[1]);
            array.add(ja);
        }

        return array;
    }
}
