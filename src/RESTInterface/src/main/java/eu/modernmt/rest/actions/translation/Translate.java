package eu.modernmt.rest.actions.translation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.engine.MMTServer;
import eu.modernmt.rest.RESTServer;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.JSONObjectAction;
import eu.modernmt.rest.framework.routing.Route;

import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 17/12/15.
 */
@Route(aliases = "translation", method = HttpMethod.GET)
public class Translate extends JSONObjectAction {

    private RESTServer server = RESTServer.getInstance();

    @Override
    protected JsonObject execute(RESTRequest req, Parameters _params) throws IOException {
        Params params = (Params) _params;

        long session;
        String translation;

        MMTServer mmtServer = server.getMMTServer();
        if (params.sessionId > 0) {
            translation = mmtServer.translate(params.query, params.sessionId, params.textProcessing);
            session = params.sessionId;
        } else if (params.context != null) {
            translation = mmtServer.translate(params.query, params.context, params.textProcessing);
            session = mmtServer.createTranslationSession(params.context).getId();
        } else {
            translation = mmtServer.translate(params.query, params.textProcessing);
            session = 0L;
        }

        JsonObject result = new JsonObject();
        result.addProperty("translation", translation);
        if (session > 0)
            result.addProperty("session", session);
        return result;
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final String query;
        public final long sessionId;
        public final List<ContextDocument> context;
        public final boolean textProcessing;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            textProcessing = getBoolean("processing", true);
            query = getString("q", false);
            sessionId = getLong("session", 0L);

            if (sessionId == 0) {
                JsonArray json = getJSONArray("context", null);

                if (json != null) {
                    try {
                        context = CreateTranslationSession.parseContext(json);
                    } catch (JsonParseException e) {
                        throw new ParameterParsingException("context", json.toString(), e);
                    }
                } else {
                    context = null;
                }
            } else {
                context = null;
            }
        }
    }
}
