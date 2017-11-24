package eu.modernmt.rest.framework.actions;

import com.google.gson.JsonElement;
import eu.modernmt.cluster.error.SystemShutdownException;
import eu.modernmt.facade.exceptions.AuthenticationException;
import eu.modernmt.facade.exceptions.TranslationRejectedException;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.RESTResponse;
import eu.modernmt.rest.framework.routing.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class JSONAction implements Action {

    protected final Logger logger = LogManager.getLogger(getClass());

    @Override
    public final void execute(RESTRequest req, RESTResponse resp) {
        try {
            unsecureExecute(req, resp);
        } catch (TemplateException e) {
            if (logger.isDebugEnabled())
                logger.debug("Template exception while executing action " + this, e);
            resp.resourceNotFound();
        } catch (Parameters.ParameterParsingException e) {
            resp.badRequest(e);
        } catch (UnsupportedLanguageException e) {
            if (logger.isDebugEnabled())
                logger.debug("Language direction '" + e.getLanguagePair() + "' is not supported " + this, e);
            resp.badRequest(e);
        } catch (AuthenticationException e) {
            if (logger.isDebugEnabled())
                logger.debug("Authentication exception while executing action " + this, e);
            resp.forbidden(e);
        } catch (SystemShutdownException e) {
            if (logger.isDebugEnabled())
                logger.debug("Unable to complete action " + this + ": system is shutting down", e);
            resp.unavailable(e);
        } catch (TranslationRejectedException e) {
            resp.unavailable(e);
        } catch (Throwable e) {
            logger.error("Internal error while executing action " + this, e);
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
