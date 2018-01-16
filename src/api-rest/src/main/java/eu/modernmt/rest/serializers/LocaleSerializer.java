package eu.modernmt.rest.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import eu.modernmt.lang.Language;

import java.lang.reflect.Type;

/**
 * Created by davide on 17/12/15.
 */
public class LocaleSerializer implements JsonSerializer<Language> {

    @Override
    public JsonElement serialize(Language src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toLanguageTag());
    }
}
