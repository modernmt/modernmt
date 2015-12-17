package eu.modernmt.rest.framework.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import eu.modernmt.rest.framework.JSONSerializer;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;

public class ObjectActionResult<M> extends JSONActionResult {

    private M object;
    private Class<?> type;

    public ObjectActionResult(M object, Class<M> type) {
        this.object = object;
        this.type = type;
    }

    @Override
    public JsonElement dump(JSONAction action, RESTRequest req, Parameters params) throws JsonParseException {
        JsonElement json = JSONSerializer.toJSON(object, type);
        action.decorate(json);
        return json;
    }

}
