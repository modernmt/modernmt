package eu.modernmt.cli;

import eu.modernmt.engine.MasterNode;
import eu.modernmt.engine.TranslationEngine;
import eu.modernmt.rest.RESTServer;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by davide on 17/12/15.
 */
public class RESTMain {

    private static final Options cliOptions;

    static {
        Option engine = Option.builder("e").longOpt("engine").hasArg().required().build();
        Option restPort = Option.builder("a").longOpt("api-port").hasArg().type(Integer.class).required().build();
        Option clusterPorts = Option.builder("p").longOpt("cluster-ports").hasArgs().numberOfArgs(2).type(Integer.class).required().build();

        cliOptions = new Options();
        cliOptions.addOption(engine);
        cliOptions.addOption(restPort);
        cliOptions.addOption(clusterPorts);
    }

    public static void main(String[] args) throws Throwable {
        CommandLineParser parser = new DefaultParser();
        CommandLine cli = parser.parse(cliOptions, args);

        TranslationEngine engine = new TranslationEngine(cli.getOptionValue("engine"));
        engine.ensure();

        String[] sPorts = cli.getOptionValues("cluster-ports");
        int[] ports = new int[]{Integer.parseInt(sPorts[0]), Integer.parseInt(sPorts[1])};
        MasterNode masterNode = new MasterNode(engine, ports);

        int port = Integer.parseInt(cli.getOptionValue("api-port"));
        RESTServer.setup(port, masterNode);

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        RESTServer server = RESTServer.getInstance();
        server.start();
    }

    public static class ShutdownHook extends Thread {

        protected final Logger logger = LoggerFactory.getLogger(getClass());

        @Override
        public void run() {
            logger.info("Received KILL signal, stopping server.");
            RESTServer server = RESTServer.getInstance();

            try {
                server.stop();
                server.join();
                logger.info("Server terminated successfully");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
