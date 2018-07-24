package eu.modernmt.api.framework.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;

public class JSONObjectActionResult extends JSONActionResult {

    private JsonObject object;

    public JSONObjectActionResult(JsonObject object) {
        this.object = object;
    }

    @Override
    public JsonElement dump(JSONAction action, RESTRequest req, Parameters params) throws JsonParseException {
        action.decorate(object);
        return object;
    }
}
