package eu.modernmt.api.framework.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;

public class JSONArrayActionResult extends JSONActionResult {

    private JsonArray array;

    public JSONArrayActionResult(JsonArray array) {
        this.array = array;
    }

    @Override
    public JsonElement dump(JSONAction action, RESTRequest req, Parameters params) throws JsonParseException {
        for (int i = 0; i < array.size(); i++)
            action.decorate(array.get(i));

        return array;
    }

}
