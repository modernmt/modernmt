package eu.modernmt.rest.framework;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by davide on 15/12/15.
 */
public class RESTResponse {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private HttpServletResponse response;

    public RESTResponse(HttpServletResponse response) {
        this.response = response;
    }

    private static String encode(Throwable e) {
        if (e == null)
            return null;

        // Message
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            Throwable cause = e.getCause();
            if (cause != null)
                msg = cause.getMessage();
        }

        msg = msg == null ? null : msg.trim();

        // Type
        String type = e.getClass().getSimpleName();

        // Code
        int code = 0;
        if (e instanceof ClientException)
            code = ((ClientException) e).getCode();

        // Encoding
        JsonObject json = new JsonObject();
        JsonObject error = new JsonObject();
        json.add("error", error);

        error.addProperty("type", type);
        if (msg != null)
            error.addProperty("message", msg);
        if (code > 0)
            error.addProperty("code", code);

        return json.toString();
    }

    public void resourceNotFound() {
        resourceNotFound(null);
    }

    public void resourceNotFound(Throwable e) {
        output(HttpServletResponse.SC_NOT_FOUND, encode(e));
    }

    public void badRequest() {
        badRequest(null);
    }

    public void badRequest(Throwable e) {
        output(HttpServletResponse.SC_BAD_REQUEST, encode(e));
    }

    public void ok() {
        ok(new JsonObject());
    }

    public void ok(JsonElement json) {
        output(HttpServletResponse.SC_OK, json.toString());
    }

    public void forbidden() {
        forbidden(null);
    }

    public void forbidden(Throwable e) {
        output(HttpServletResponse.SC_FORBIDDEN, encode(e));
    }

    public void unexpectedError() {
        unexpectedError(null);
    }

    public void unexpectedError(Throwable e) {
        output(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, encode(e));
    }

    public void unavailable() {
        unavailable(null);
    }

    public void unavailable(Throwable e) {
        output(HttpServletResponse.SC_SERVICE_UNAVAILABLE, encode(e));
    }

    private void output(int httpStatus, String json) {
        if (logger.isDebugEnabled()) {
            if (json.length() > 200) {
                logger.trace("response content: " + json.substring(0, 199)
                        + "...");
            } else {
                logger.trace("response content: " + json);
            }
        }

        response.setStatus(httpStatus);
        response.setContentType("application/json; charset=utf-8");

        try {
            response.getOutputStream().write(json.getBytes("UTF-8"));
        } catch (IOException e) {
            logger.error("unable to write response", e);
        }
    }

    public int getHttpStatus() {
        return response.getStatus();
    }

}

