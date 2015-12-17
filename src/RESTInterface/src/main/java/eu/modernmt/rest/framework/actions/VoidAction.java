package eu.modernmt.rest.framework.actions;

import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;

public abstract class VoidAction extends JSONAction {

    @Override
    protected final JSONActionResult getResult(RESTRequest req, Parameters params) throws Throwable {
        try {
            execute(req, params);
            return new VoidActionResult();
        } catch (NotFoundException e) {
            return null;
        }
    }

    protected abstract void execute(RESTRequest req, Parameters params) throws Throwable;

    protected static class NotFoundException extends Exception {

        public NotFoundException() {
        }

    }

}
