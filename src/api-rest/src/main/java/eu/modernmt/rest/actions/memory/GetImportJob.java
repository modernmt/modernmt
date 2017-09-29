package eu.modernmt.rest.actions.memory;

import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.ImportJob;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.framework.routing.TemplateException;

import java.util.UUID;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "memories/imports/:id", method = HttpMethod.GET)
public class GetImportJob extends ObjectAction<ImportJob> {

    @Override
    protected ImportJob execute(RESTRequest req, Parameters _params) throws PersistenceException {
        Params params = (Params) _params;
        return ModernMT.memory.getImportJob(params.id);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        private final UUID id;

        public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
            super(req);
            id = req.getPathParameterAsUUID("id");
        }
    }

}
