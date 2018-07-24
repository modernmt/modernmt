package eu.modernmt.api.framework.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;

public abstract class JSONActionResult {

    public void beforeDump(RESTRequest req, Parameters params) throws Throwable {
        // Default implementation does nothing
    }

    public abstract JsonElement dump(JSONAction action, RESTRequest req, Parameters params) throws JsonParseException;

}
