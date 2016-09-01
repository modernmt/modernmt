package eu.modernmt.rest.framework.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;

public abstract class JSONActionResult {

    public void beforeDump(RESTRequest req, Parameters params) throws Throwable {
        // Default implementation does nothing
    }

    public abstract JsonElement dump(JSONAction action, RESTRequest req, Parameters params) throws JsonParseException;

}
