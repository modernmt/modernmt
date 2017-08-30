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
@Route(aliases = "domains/:id", method = HttpMethod.PUT)
public class UpdateDomain extends ObjectAction<Domain> {

    @Override
    protected Domain execute(RESTRequest req, Parameters _params) throws Throwable {
        Params params = (Params) _params;
        return ModernMT.domain.update(new Domain(params.id, params.name));
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        private final long id;
        private final String name;

        public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
            super(req);
            id = req.getPathParameterAsLong("id");
            name = getString("name", false);
        }
    }

}