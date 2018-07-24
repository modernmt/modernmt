package eu.modernmt.api.framework.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import eu.modernmt.api.framework.JSONSerializer;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;

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
