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
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.model.TranslationResult;

import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 17/12/15.
 */
@Route(aliases = "translation", method = HttpMethod.GET)
public class Translate extends ObjectAction<TranslationResult> {

    private RESTServer server = RESTServer.getInstance();

    @Override
    protected TranslationResult execute(RESTRequest req, Parameters _params) throws IOException {
        Params params = (Params) _params;
        MMTServer mmtServer = server.getMMTServer();

        TranslationResult result = new TranslationResult();

        if (params.sessionId > 0) {
            result.translation = mmtServer.translate(params.query, params.sessionId, params.textProcessing);
            result.session = params.sessionId;
        } else if (params.context != null) {
            result.translation = mmtServer.translate(params.query, params.context, params.textProcessing);
        } else if (params.contextString != null) {
            result.context = mmtServer.getContext(params.contextString, params.contextLimit);
            System.out.println(result.context);
            result.translation = mmtServer.translate(params.query, result.context, params.textProcessing);
        } else {
            result.translation = mmtServer.translate(params.query, params.textProcessing);
        }

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
        public final String contextString;
        public final int contextLimit;
        public final boolean textProcessing;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            textProcessing = getBoolean("processing", true);
            query = getString("q", false);
            sessionId = getLong("session", 0L);
            contextLimit = getInt("context_limit", 10);

            if (sessionId == 0) {
                JsonArray json = getJSONArray("context", null);

                if (json != null) {
                    try {
                        context = CreateTranslationSession.parseContext(json);
                    } catch (JsonParseException e) {
                        throw new ParameterParsingException("context", json.toString(), e);
                    }
                    contextString = null;
                } else {
                    context = null;
                    contextString = getString("context_string", false, null);
                }
            } else {
                context = null;
                contextString = null;
            }
        }
    }
}
