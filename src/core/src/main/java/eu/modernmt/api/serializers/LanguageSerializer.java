package eu.modernmt.api.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import eu.modernmt.lang.Language2;

import java.lang.reflect.Type;

/**
 * Created by davide on 17/12/15.
 */
public class LanguageSerializer implements JsonSerializer<Language2> {

    @Override
    public JsonElement serialize(Language2 src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toLanguageTag());
    }
}
