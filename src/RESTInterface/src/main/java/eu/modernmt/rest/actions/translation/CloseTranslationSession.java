package eu.modernmt.rest.actions.translation;

import eu.modernmt.core.facade.ModernMT;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.VoidAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.framework.routing.TemplateException;

import java.io.IOException;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "sessions/:id", method = HttpMethod.DELETE)
public class CloseTranslationSession extends VoidAction {

    @Override
    protected void execute(RESTRequest req, Parameters _params) throws NotFoundException, IOException {
        Params params = (Params) _params;

        TranslationSession session = ModernMT.decoder.getSession(params.id);
        if (session == null)
            throw new NotFoundException();

        session.close();
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
