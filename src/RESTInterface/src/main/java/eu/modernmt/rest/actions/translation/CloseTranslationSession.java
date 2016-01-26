package eu.modernmt.rest.actions.translation;

import eu.modernmt.rest.RESTServer;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.VoidAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.framework.routing.TemplateException;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "sessions/:id", method = HttpMethod.DELETE)
public class CloseTranslationSession extends VoidAction {

    private RESTServer server = RESTServer.getInstance();

    @Override
    protected void execute(RESTRequest req, Parameters _params) {
        Params params = (Params) _params;
        server.getMasterNode().closeTranslationSession(params.id);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final long id;

        public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
            super(req);
            id = req.getPathParameterAsLong("id");
        }
    }
}
