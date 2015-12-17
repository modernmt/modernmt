package eu.modernmt.rest.actions.translation;

import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.Sentence;
import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.engine.MMTServer;
import eu.modernmt.rest.RESTServer;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.CollectionAction;
import eu.modernmt.rest.framework.routing.Route;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Created by davide on 17/12/15.
 */
@Route(aliases = "translation/nbest", method = HttpMethod.GET)
public class GetNBestTranslations extends CollectionAction<TranslationHypothesis> {

    private RESTServer server = RESTServer.getInstance();

    @Override
    protected Collection<TranslationHypothesis> execute(RESTRequest req, Parameters _params) throws IOException {
        Params params = (Params) _params;
        Sentence query = new Sentence(params.query);

        MMTServer mmtServer = server.getMMTServer();
        if (params.sessionId > 0)
            return mmtServer.translate(query, params.sessionId, params.nbest);
        else if (params.context != null)
            return mmtServer.translate(query, params.context, params.nbest);
        else
            return mmtServer.translate(query, params.nbest);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final String query;
        public final long sessionId;
        public final List<ContextDocument> context;
        public final int nbest;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            nbest = getInt("nbest");
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
