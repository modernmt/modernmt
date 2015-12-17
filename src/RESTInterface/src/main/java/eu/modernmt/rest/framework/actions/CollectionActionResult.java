package eu.modernmt.rest.framework.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import eu.modernmt.rest.framework.JSONSerializer;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;

import java.util.Collection;

public class CollectionActionResult<M> extends JSONActionResult {

    private Collection<M> collection;
    private Class<M> type;

    public CollectionActionResult(Collection<M> collection, Class<M> type) {
        this.collection = collection;
        this.type = type;
    }

    @Override
    public JsonElement dump(JSONAction action, RESTRequest req, Parameters params) throws JsonParseException {
        JsonArray array = new JsonArray();

        for (Object element : collection) {
            JsonElement json = JSONSerializer.toJSON(element, type);
            action.decorate(json);
            array.add(json);
        }

        return array;
    }

}
