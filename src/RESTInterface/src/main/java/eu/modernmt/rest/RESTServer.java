package eu.modernmt.rest;

import eu.modernmt.config.Config;
import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.engine.MasterNode;
import eu.modernmt.rest.framework.JSONSerializer;
import eu.modernmt.rest.framework.routing.RouterServlet;
import eu.modernmt.rest.model.TranslationResponse;
import eu.modernmt.rest.serializers.TranslationHypothesisSerializer;
import eu.modernmt.rest.serializers.TranslationResponseSerializer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 15/12/15.
 */
public class RESTServer {

    static {
        JSONSerializer.registerCustomSerializer(TranslationHypothesis.class, new TranslationHypothesisSerializer());
        JSONSerializer.registerCustomSerializer(TranslationResponse.class, new TranslationResponseSerializer());
    }

    private static RESTServer instance = null;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static void setup(int restPort, MasterNode masterNode) {
        if (instance != null)
            throw new IllegalStateException("Setup has been called twice.");

        instance = new RESTServer(restPort, masterNode);
    }

    public static RESTServer getInstance() {
        if (instance == null)
            throw new IllegalStateException("You must call setup first.");

        return instance;
    }

    private Server jettyServer;
    private MasterNode masterNode;

    private RESTServer(int restPort, MasterNode masterNode) {
        this.masterNode = masterNode;
        this.jettyServer = new Server(restPort);

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(Router.class, "/*");
        jettyServer.setHandler(handler);
    }

    public void start() throws Exception {
        masterNode.start();
        jettyServer.start();
    }

    public void stop() throws Exception {
        masterNode.shutdown();
        jettyServer.stop();
    }

    public void join() throws InterruptedException {
        masterNode.awaitTermination(1, TimeUnit.DAYS);
        jettyServer.join();
    }

    public MasterNode getMasterNode() {
        return masterNode;
    }

    public static class Router extends RouterServlet {

        @Override
        protected Collection<Class<?>> getDeclaredActions() throws ServletException {
            InputStream stream = null;

            try {
                ArrayList<Class<?>> classes = new ArrayList<>();

                stream = getClass().getClassLoader().getResourceAsStream("rest-actions.list");
                LineIterator lines = IOUtils.lineIterator(stream, Config.charset.get());

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
