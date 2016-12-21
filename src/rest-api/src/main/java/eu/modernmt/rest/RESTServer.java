package eu.modernmt.rest;

import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.io.DefaultCharset;
import eu.modernmt.model.Alignment;
import eu.modernmt.rest.framework.JSONSerializer;
import eu.modernmt.rest.framework.routing.RouterServlet;
import eu.modernmt.rest.model.TranslationResponse;
import eu.modernmt.rest.serializers.AlignmentSerializer;
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
        JSONSerializer.registerCustomSerializer(Alignment.class, new AlignmentSerializer());
    }

    public static class ServerOptions {

        // The network port to bind
        public int port;

        // Directory used for storing uploaded files
        public File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"));

        // The maximum size allowed for uploaded files
        public long maxFileSize = 100L * 1024L * 1024L; // 100mb

        // The maximum size allowed for multipart/form-data requests
        public long maxRequestSize = 101L * 1024L * 1024L; // 101mb

        // the size threshold after which files will be written to disk
        public int fileSizeThreshold = 2 * 1024; // 2kb

        public ServerOptions(int port) {
            this.port = port;
        }
    }

    private Server jettyServer;

    public RESTServer(int port) {
        this(new ServerOptions(port));
    }

    public RESTServer(ServerOptions options) {
        this.jettyServer = new Server(options.port);

        ServletHandler router = new ServletHandler();
        router.addServletWithMapping(Router.class, "/*");

        MultipartConfigInjectionHandler handlerWrapper = new MultipartConfigInjectionHandler(
                options.temporaryDirectory, options.maxFileSize, options.maxRequestSize, options.fileSizeThreshold);
        handlerWrapper.setHandler(router);

        jettyServer.setHandler(handlerWrapper);
    }

    public void start() throws Exception {
        jettyServer.start();

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
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
                LineIterator lines = IOUtils.lineIterator(stream, DefaultCharset.get());

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

    private class ShutdownHook extends Thread {

        @Override
        public void run() {
            try {
                RESTServer.this.stop();
            } catch (Throwable e) {
                // Ignore
            }

            try {
                RESTServer.this.join();
            } catch (Throwable e) {
                // Ignore
            }
        }

    }

}
