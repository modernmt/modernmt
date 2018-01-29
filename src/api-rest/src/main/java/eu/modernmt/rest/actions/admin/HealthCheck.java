package eu.modernmt.rest.actions.admin;


import eu.modernmt.facade.ModernMT;
import eu.modernmt.facade.exceptions.TestFailedException;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.VoidAction;
import eu.modernmt.rest.framework.routing.Route;

@Route(aliases = "_health", method = HttpMethod.GET, log = false)
public class HealthCheck extends VoidAction {

    @Override
    protected void execute(RESTRequest req, Parameters params) throws TestFailedException {
        ModernMT.test();
    }

}
