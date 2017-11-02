package eu.modernmt.rest.framework;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Created by davide on 17/12/15.
 */
public class JSONSerializer {

    private static final GsonBuilder builder = new GsonBuilder();
    private static Gson customInstance = null;
    private static Gson staticInstance = new Gson();

    public static void registerCustomSerializer(Class<?> clazz, JsonSerializer<?> serializer) {
        builder.registerTypeAdapter(clazz, serializer);
    }

    private static Gson getCustom() {
        if (customInstance == null) {
            synchronized (JSONSerializer.class) {
                if (customInstance == null)
                    customInstance = builder.create();
            }
        }

        return customInstance;
    }

    public static JsonElement toJSON(Object object, Type type) {
        return toJSON(object, type, true);
    }

    public static JsonElement toJSON(Object object, Type type, boolean custom) {
        Gson gson = custom ? getCustom() : staticInstance;
        return gson.toJsonTree(object, type);
    }

}
