package eu.modernmt.api.framework.actions;

import com.google.gson.JsonObject;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;

public abstract class JSONObjectAction extends JSONAction {

    @Override
    protected final JSONActionResult getResult(RESTRequest req, Parameters params) throws Throwable {
        JsonObject json = execute(req, params);
        return json == null ? null : new JSONObjectActionResult(json);
    }

    protected abstract JsonObject execute(RESTRequest req, Parameters params) throws Throwable;

}
