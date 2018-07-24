package eu.modernmt.api.framework.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;

public class VoidActionResult extends JSONActionResult {

    public static VoidActionResult INSTANCE = new VoidActionResult();

    private VoidActionResult() {
    }

    @Override
    public JsonElement dump(JSONAction action, RESTRequest req, Parameters params) throws JsonParseException {
        return null;
    }

}
