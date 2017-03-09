package eu.modernmt.rest.actions.domain;

import eu.modernmt.data.DataManagerException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.VoidAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.framework.routing.TemplateException;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "domains/:id", method = HttpMethod.DELETE)
public class DeleteDomain extends VoidAction {

    @Override
    protected void execute(RESTRequest req, Parameters _params) throws PersistenceException, NotFoundException, DataManagerException {
        Params params = (Params) _params;
        if (!ModernMT.domain.delete(params.id))
            throw new NotFoundException();
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