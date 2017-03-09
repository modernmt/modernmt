package eu.modernmt.rest.framework.actions;

import com.google.gson.JsonObject;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;

public abstract class JSONObjectAction extends JSONAction {

    @Override
    protected final JSONActionResult getResult(RESTRequest req, Parameters params) throws Throwable {
        JsonObject json = execute(req, params);
        return json == null ? null : new JSONObjectActionResult(json);
    }

    protected abstract JsonObject execute(RESTRequest req, Parameters params) throws Throwable;

}
