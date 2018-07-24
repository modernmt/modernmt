package eu.modernmt.api.actions;


import eu.modernmt.facade.ModernMT;
import eu.modernmt.facade.exceptions.TestFailedException;
import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.VoidAction;
import eu.modernmt.api.framework.routing.Route;

@Route(aliases = "_health", method = HttpMethod.GET, log = false)
public class HealthCheck extends VoidAction {

    @Override
    protected void execute(RESTRequest req, Parameters params) throws TestFailedException {
        ModernMT.test();
    }

}
