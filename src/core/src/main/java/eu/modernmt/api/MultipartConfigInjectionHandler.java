package eu.modernmt.api;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.MultiPartInputStreamParser;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 21/12/16.
 */
class MultipartConfigInjectionHandler extends HandlerWrapper {

    private static final String MULTIPART_FORMDATA_TYPE = "multipart/form-data";

    private final MultipartConfigElement multipartConfig;

    public MultipartConfigInjectionHandler(File location, long maxFileSize, long maxRequestSize, int fileSizeThreshold) {
        this.multipartConfig = new MultipartConfigElement(location.getAbsolutePath(), maxFileSize, maxRequestSize, fileSizeThreshold);
    }

    private void enableMultipartSupport(HttpServletRequest request) {
        request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfig);
    }

    private boolean isMultipartRequest(ServletRequest request) {
        return request.getContentType() != null && request.getContentType().startsWith(MULTIPART_FORMDATA_TYPE);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
        String method = request.getMethod();

        boolean multipartRequest = (HttpMethod.POST.is(method) || HttpMethod.PUT.is(method)) && isMultipartRequest(request);

        if (multipartRequest)
            enableMultipartSupport(request);

        try {
            super.handle(target, baseRequest, request, response);
        } finally {
            if (multipartRequest) {
                MultiPartInputStreamParser multipartInputStream = (MultiPartInputStreamParser) request
                        .getAttribute(Request.__MULTIPART_INPUT_STREAM);
                if (multipartInputStream != null) {
                    try {
                        // A multipart request to a servlet will have the parts cleaned up correctly, but
                        // the repeated call to deleteParts() here will safely do nothing.
                        multipartInputStream.deleteParts();
                    } catch (MultiException e) {
                        // Ignore it
                    }
                }
            }
        }
    }

}
