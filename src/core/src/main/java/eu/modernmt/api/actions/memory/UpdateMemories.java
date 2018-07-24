package eu.modernmt.api.actions.memory;

import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.Memory;
import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.ObjectAction;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.api.framework.routing.TemplateException;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = {"memories/:id", "domains/:id"}, method = HttpMethod.PUT)
public class UpdateMemories extends ObjectAction<Memory> {

    @Override
    protected Memory execute(RESTRequest req, Parameters _params) throws Throwable {
        Params params = (Params) _params;
        return ModernMT.memory.update(new Memory(params.id, params.name));
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