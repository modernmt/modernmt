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
    private static Gson instance = null;

    public static void registerCustomSerializer(Class<?> clazz, JsonSerializer<?> serializer) {
        builder.registerTypeAdapter(clazz, serializer);
    }

    private static Gson get() {
        if (instance == null) {
            synchronized (JSONSerializer.class) {
                if (instance == null)
                    instance = builder.create();
            }
        }

        return instance;
    }

    public static JsonElement toJSON(Object object,  Type type) {
        return get().toJsonTree(object, type);
    }

}
