package eu.modernmt.rest.framework.routing;

import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.RESTResponse;
import eu.modernmt.rest.framework.actions.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

public abstract class RouterServlet extends HttpServlet {

    private static final String DEFAULT_ENCODING = "UTF-8";

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private RouteTree routes;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        logger.info("Loading action lists...");

        routes = new RouteTree();

        for (Class<?> clazz : getDeclaredActions()) {
            if (!Action.class.isAssignableFrom(clazz))
                continue;

            Class<? extends Action> actionClass = clazz.asSubclass(Action.class);

            Route route = actionClass.getAnnotation(Route.class);
            if (route != null) {
                HttpMethod method = route.method();

                for (String path : route.aliases()) {
                    RouteTemplate template = new RouteTemplate('/' + path, actionClass, method);
                    routes.add(template);
                    logger.info("Servlet found: " + template);
                }
            }
        }
    }

    protected abstract Collection<Class<?>> getDeclaredActions() throws ServletException;

    private RESTRequest wrapRequest(HttpServletRequest req) {
        // Character Encoding
        String encoding = req.getCharacterEncoding();
        if (encoding == null)
            try {
                req.setCharacterEncoding(DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Invalid DEFAULT_ENCODING", e);
            }

        // Wrap
        RESTRequest wrapper = new RESTRequest(req, routes);

        if (logger.isDebugEnabled()) {
            logger.debug("Request path: " + wrapper.getHttpMethod() + " " + wrapper.getPath());
            logger.debug("Found template: " + wrapper.getTemplate());
            logger.debug("Found action: " + wrapper.getActionClass().getSimpleName());
        }

        return wrapper;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        RESTRequest restRequest = wrapRequest(req);
        RESTResponse restResponse = new RESTResponse(resp);

        try {
            Class<? extends Action> actionClass = restRequest.getActionClass();

            if (actionClass == null) {
                restResponse.resourceNotFound();
            } else {
                Action action = actionClass.newInstance();

                if (logger.isDebugEnabled())
                    logger.debug("redirect to action " + action);

                action.execute(restRequest, restResponse);
            }
        } catch (Throwable e) {
            restResponse.unexpectedError(e);
        } finally {
            String method = restRequest.getHttpMethod().toString();
            String path = restRequest.getPath();
            int status = restResponse.getHttpStatus();

            logger.info(method + " /" + path + ": " + status);
        }

    }

}
