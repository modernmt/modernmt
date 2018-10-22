package eu.modernmt.api.actions;


import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.VoidAction;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.facade.exceptions.TestFailedException;

@Route(aliases = "_health", method = HttpMethod.GET, log = false)
public class HealthCheck extends VoidAction {

    @Override
    protected void execute(RESTRequest req, Parameters _params) throws TestFailedException {
        Params params = (Params) _params;
        ModernMT.test(params.strict);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final boolean strict;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            strict = getBoolean("strict", false);
        }
    }

}
