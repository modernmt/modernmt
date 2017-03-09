package eu.modernmt.rest.framework;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by davide on 15/12/15.
 */
public class RESTResponse {

    protected final Logger logger = LogManager.getLogger(getClass());

    private HttpServletResponse response;
    private JsonObject content = null;

    public RESTResponse(HttpServletResponse response) {
        this.response = response;
    }

    public void resourceNotFound() {
        resourceNotFound(null);
    }

    public void resourceNotFound(Throwable e) {
        output(HttpServletResponse.SC_NOT_FOUND, null, e);
    }

    public void badRequest() {
        badRequest(null);
    }

    public void badRequest(Throwable e) {
        output(HttpServletResponse.SC_BAD_REQUEST, null, e);
    }

    public void ok() {
        ok(new JsonObject());
    }

    public void ok(JsonElement json) {
        output(HttpServletResponse.SC_OK, json, null);
    }

    public void forbidden() {
        forbidden(null);
    }

    public void forbidden(Throwable e) {
        output(HttpServletResponse.SC_FORBIDDEN, null, e);
    }

    public void unexpectedError() {
        unexpectedError(null);
    }

    public void unexpectedError(Throwable e) {
        output(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, e);
    }

    public void unavailable() {
        unavailable(null);
    }

    public void unavailable(Throwable e) {
        output(HttpServletResponse.SC_SERVICE_UNAVAILABLE, null, e);
    }

    private void output(int httpStatus, JsonElement json, Throwable throwable) {
        if (content != null)
            throw new IllegalStateException("Output has been already set");

        content = new JsonObject();
        content.addProperty("status", httpStatus);

        if (throwable != null)
            content.add("error", encode(throwable));
        else if (json != null)
            content.add("data", json);

        response.setStatus(httpStatus);
        response.setContentType("application/json; charset=utf-8");

        try {
            if (content != null)
                response.getOutputStream().write(content.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            logger.error("unable to write response", e);
        }
    }

    private static JsonObject encode(Throwable e) {
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

//        // Code
//        int code = 0;
//        if (e instanceof ClientException)
//            code = ((ClientException) e).getCode();

        // Encoding
        JsonObject error = new JsonObject();

        error.addProperty("type", type);
        if (msg != null)
            error.addProperty("message", msg);
//        if (code > 0)
//            error.addProperty("code", code);

        return error;
    }

    public int getHttpStatus() {
        return response.getStatus();
    }

    public JsonElement getContent() {
        return content;
    }
}

