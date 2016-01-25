package eu.modernmt.cli;

import eu.modernmt.engine.MMTServer;
import eu.modernmt.engine.TranslationEngine;
import eu.modernmt.rest.RESTServer;
import org.apache.commons.cli.*;

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
        MMTServer mmtServer = new MMTServer(engine, ports);

        int port = Integer.parseInt(cli.getOptionValue("api-port"));
        RESTServer.setup(port, mmtServer);

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        RESTServer server = RESTServer.getInstance();
        server.start();
    }

    public static class ShutdownHook extends Thread {

        @Override
        public void run() {
            RESTServer server = RESTServer.getInstance();

            try {
                server.stop(true);
                server.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
