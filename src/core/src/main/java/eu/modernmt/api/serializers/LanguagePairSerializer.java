package eu.modernmt.api.serializers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import eu.modernmt.lang.LanguageDirection;

import java.lang.reflect.Type;

/**
 * Created by davide on 17/12/15.
 */
public class LanguagePairSerializer implements JsonSerializer<LanguageDirection> {

    @Override
    public JsonElement serialize(LanguageDirection pair, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();
        array.add(pair.source.toLanguageTag());
        array.add(pair.target.toLanguageTag());
        return array;
    }
}
