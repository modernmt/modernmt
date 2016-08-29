package eu.modernmt.rest;

import eu.modernmt.constants.Const;
import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.rest.framework.JSONSerializer;
import eu.modernmt.rest.framework.routing.RouterServlet;
import eu.modernmt.rest.model.TranslationResponse;
import eu.modernmt.rest.serializers.TranslationHypothesisSerializer;
import eu.modernmt.rest.serializers.TranslationResponseSerializer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

import javax.servlet.ServletException;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by davide on 15/12/15.
 */
public class RESTServer {

    static {
        JSONSerializer.registerCustomSerializer(TranslationResponse.class, new TranslationResponseSerializer());
        JSONSerializer.registerCustomSerializer(TranslationHypothesis.class, new TranslationHypothesisSerializer());
    }

    private Server jettyServer;

    public RESTServer(int port) {
        this.jettyServer = new Server(port);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(Router.class, "/*");
        jettyServer.setHandler(handler);
    }

    public void start() throws Exception {
        jettyServer.start();
    }

    public void stop() throws Exception {
        jettyServer.stop();
    }

    public void join() throws InterruptedException {
        jettyServer.join();
    }

    public static class Router extends RouterServlet {

        @Override
        protected Collection<Class<?>> getDeclaredActions() throws ServletException {
            InputStream stream = null;

            try {
                ArrayList<Class<?>> classes = new ArrayList<>();

                stream = getClass().getClassLoader().getResourceAsStream("rest-actions.list");
                LineIterator lines = IOUtils.lineIterator(stream, Const.charset.get());

                while (lines.hasNext()) {
                    String line = lines.nextLine();
                    String className = line.replace(File.separatorChar, '.').replace(".java", "");

                    classes.add(Class.forName(className));
                }

                return classes;
            } catch (Throwable e) {
                throw new ServletException("Error during actions list loading", e);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
    }

}
