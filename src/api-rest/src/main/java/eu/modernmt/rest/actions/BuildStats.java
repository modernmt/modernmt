package eu.modernmt.rest.actions;

import com.google.gson.JsonObject;
import eu.modernmt.Pom;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.JSONObjectAction;
import eu.modernmt.rest.framework.routing.Route;

import java.io.IOException;

/**
 * Created by davide on 23/12/15.
 */
@Route(aliases = "_build", method = HttpMethod.GET)
public class BuildStats extends JSONObjectAction {

    @Override
    protected JsonObject execute(RESTRequest req, Parameters params) throws IOException {
        JsonObject result = new JsonObject();
        result.addProperty("version", Pom.getProperty("mmt.version"));
        result.addProperty("number", Pom.getProperty("mmt.build.number"));

        return result;
    }

}
