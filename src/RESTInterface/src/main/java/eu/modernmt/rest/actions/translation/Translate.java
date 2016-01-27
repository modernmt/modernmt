package eu.modernmt.rest.actions.translation;

import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.engine.MasterNode;
import eu.modernmt.engine.TranslationException;
import eu.modernmt.rest.RESTServer;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.model.TranslationResponse;

import java.util.List;

/**
 * Created by davide on 17/12/15.
 */
@Route(aliases = "translate", method = HttpMethod.GET)
public class Translate extends ObjectAction<TranslationResponse> {

    private RESTServer server = RESTServer.getInstance();

    @Override
    protected TranslationResponse execute(RESTRequest req, Parameters _params) throws ContextAnalyzerException, TranslationException {
        Params params = (Params) _params;
        MasterNode masterNode = server.getMasterNode();

        TranslationResponse result = new TranslationResponse();

        if (params.sessionId > 0) {
            result.session = params.sessionId;
            result.translation = masterNode.translate(params.query, params.sessionId, params.textProcessing, params.nbest);
        } else if (params.context != null) {
            result.translation = masterNode.translate(params.query, params.context, params.textProcessing, params.nbest);
        } else if (params.contextString != null) {
            result.context = masterNode.getContext(params.contextString, params.contextLimit);
            result.translation = masterNode.translate(params.query, result.context, params.textProcessing, params.nbest);
        } else {
            result.translation = masterNode.translate(params.query, params.textProcessing, params.nbest);
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
        public final int nbest;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            textProcessing = getBoolean("processing", true);
            query = getString("q", false);
            sessionId = getLong("session", 0L);
            contextLimit = getInt("context_limit", 10);
            nbest = getInt("nbest", 0);

            if (sessionId == 0) {
                JsonArray json = getJSONArray("context_array", null);

                if (json != null) {
                    try {
                        context = CreateTranslationSession.parseContext(json);
                    } catch (JsonParseException e) {
                        throw new ParameterParsingException("context_array", json.toString(), e);
                    }
                    contextString = null;
                } else {
                    context = null;
                    contextString = getString("context", false, null);
                }
            } else {
                context = null;
                contextString = null;
            }
        }
    }
}
