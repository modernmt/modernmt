package eu.modernmt.rest.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class LocaleSerializer implements JsonSerializer<Locale> {

    @Override
    public JsonElement serialize(Locale src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toLanguageTag());
    }
}
