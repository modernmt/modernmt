package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.core.cluster.Client;
import eu.modernmt.core.facade.ModernMT;
import eu.modernmt.rest.RESTServer;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 17/12/15.
 */
public class RESTServerMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option engine = Option.builder("e").longOpt("engine").hasArg().required().build();
            Option restPort = Option.builder("a").longOpt("api-port").hasArg().type(Integer.class).required().build();
            Option clusterPort = Option.builder("p").longOpt("cluster-port").hasArg().type(Integer.class).required().build();
            Option verbosity = Option.builder("v").longOpt("verbosity").hasArg().type(Integer.class).required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(engine);
            cliOptions.addOption(restPort);
            cliOptions.addOption(clusterPort);
            cliOptions.addOption(verbosity);
        }

        public final String engine;
        public final int apiPort;
        public final int clusterPort;
        public final int verbosity;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            this.engine = cli.getOptionValue("engine");
            this.apiPort = Integer.parseInt(cli.getOptionValue("api-port"));
            this.clusterPort = Integer.parseInt(cli.getOptionValue("cluster-port"));

            String verbosity = cli.getOptionValue("verbosity");
            this.verbosity = verbosity == null ? 2 : Integer.parseInt(verbosity);
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        Log4jConfiguration.setup(args.verbosity);

        Client client = null;
        RESTServer restServer = null;
        boolean ready = false;

        try {
            client = new Client();
            client.joinCluster("127.0.0.1:" + args.clusterPort);

            ModernMT.setClient(client);

            restServer = new RESTServer(args.apiPort);
            restServer.start();

            ready = true;
        } finally {
            if (!ready && client != null)
                client.shutdown();
            else
                Runtime.getRuntime().addShutdownHook(new ShutdownHook(client, restServer));
        }
    }

    public static class ShutdownHook extends Thread {

        protected final Logger logger = LogManager.getLogger(getClass());
        private final Client client;
        private final RESTServer restServer;

        public ShutdownHook(Client client, RESTServer restServer) {
            this.client = client;
            this.restServer = restServer;
        }

        @Override
        public void run() {
            logger.info("Received KILL signal, stopping server.");

            try {
                client.shutdown();
            } catch (Throwable e) {
                logger.warn("Error while shutting down client.", e);
            }

            try {
                restServer.stop();
            } catch (Throwable e) {
                logger.warn("Error while shutting down REST server.", e);
            }

            try {
                client.awaitTermination(1, TimeUnit.DAYS);
            } catch (Throwable e) {
                logger.warn("Error while shutting down client.", e);
            }

            try {
                restServer.join();
            } catch (Throwable e) {
                logger.warn("Error while shutting down REST server.", e);
            }

            logger.info("REST server shutdown complete");
        }

    }
}
