package eu.modernmt.rest.actions.translation;

import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.ContextVector;
import eu.modernmt.rest.actions.util.ContextUtils;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "sessions", method = HttpMethod.POST)
public class CreateTranslationSession extends ObjectAction<TranslationSession> {

    @Override
    protected TranslationSession execute(RESTRequest req, Parameters _params) {
        Params params = (Params) _params;
        return ModernMT.decoder.openSession(params.context);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final ContextVector context;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);
            context = ContextUtils.parseParameter("context_vector", getString("context_vector", false));
        }

    }
}
