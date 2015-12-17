package eu.modernmt.rest.framework.actions;

import com.google.gson.JsonElement;
import eu.modernmt.rest.framework.*;
import eu.modernmt.rest.framework.routing.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

;

public abstract class JSONAction implements Action {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public final void execute(RESTRequest req, RESTResponse resp) {
        try {
            unsecureExecute(req, resp);
        } catch (TemplateException e) {
            if (logger.isDebugEnabled())
                logger.debug("Template exception while executing action " + this, e);
            resp.resourceNotFound(e);
        } catch (ClientException e) {
            if (logger.isDebugEnabled())
                logger.debug("Client exception while executing action " + this, e);
            resp.badRequest(e);
        } catch (AuthException e) {
            if (logger.isDebugEnabled())
                logger.debug("Auth exception while executing action " + this, e);
            resp.forbidden(e);
        } catch (Throwable e) {
            logger.error("Unexpected error while executing action " + this, e);
            resp.unexpectedError(e);
        }
    }

    protected final void unsecureExecute(RESTRequest req, RESTResponse resp) throws Throwable {
        Parameters params = getParameters(req);
        JSONActionResult result = getResult(req, params);

        if (result == null) {
            resp.resourceNotFound();
        } else {
            result.beforeDump(req, params);
            JsonElement json = result.dump(this, req, params);

            resp.ok(json);
        }
    }

    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Parameters(req);
    }

    protected abstract JSONActionResult getResult(RESTRequest req, Parameters params) throws Throwable;

    protected void decorate(JsonElement element) {
        // Default implementation does nothing
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName();
    }

}
