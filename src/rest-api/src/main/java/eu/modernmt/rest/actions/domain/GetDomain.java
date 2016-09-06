package eu.modernmt.rest.actions.domain;

import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.Domain;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.framework.routing.TemplateException;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "domains/:id", method = HttpMethod.GET)
public class GetDomain extends ObjectAction<Domain> {

    @Override
    protected Domain execute(RESTRequest req, Parameters _params) {
        Params params = (Params) _params;
        return ModernMT.domain.get(params.id);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        private final int id;

        public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
            super(req);
            id = req.getPathParameterAsInt("id");
        }
    }

}
