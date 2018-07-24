package eu.modernmt.api.actions.memory;

import eu.modernmt.data.DataManagerException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.VoidAction;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.api.framework.routing.TemplateException;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = {"memories/:id/corpus", "domains/:id/corpus"}, method = HttpMethod.DELETE)
public class DeleteMemoryCorpus extends VoidAction {

    @Override
    protected void execute(RESTRequest req, Parameters _params) throws PersistenceException, NotFoundException, DataManagerException {
        Params params = (Params) _params;
        ModernMT.memory.empty(params.id);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        private final long id;

        public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
            super(req);
            id = req.getPathParameterAsLong("id");
        }
    }

}