package eu.modernmt.rest.actions.translation;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.engine.MasterNode;
import eu.modernmt.engine.TranslationException;
import eu.modernmt.rest.RESTServer;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.model.TranslationResponse;

/**
 * Created by davide on 17/12/15.
 */
@Route(aliases = "translate", method = HttpMethod.GET)
public class AlignTags extends ObjectAction<String> {

    private RESTServer server = RESTServer.getInstance();

    @Override
    protected String execute(RESTRequest req, Parameters _params) throws ContextAnalyzerException, TranslationException {
        Params params = (Params) _params;
        MasterNode masterNode = server.getMasterNode();
        return masterNode.alignTags(params.query);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final String query;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);
            query = getString("q", true);
        }
    }
}
