package eu.modernmt.api.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import eu.modernmt.model.ImportJob;
import eu.modernmt.api.framework.JSONSerializer;

import java.lang.reflect.Type;

/**
 * Created by davide on 02/11/17.
 */
public class ImportJobSerializer implements JsonSerializer<ImportJob> {

    @Override
    public JsonElement serialize(ImportJob src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = (JsonObject) JSONSerializer.toJSON(src, Object.class, false);

        if (src != null && src.getMemory() == 0L)
            json.remove("memory");

        return json;
    }

}
