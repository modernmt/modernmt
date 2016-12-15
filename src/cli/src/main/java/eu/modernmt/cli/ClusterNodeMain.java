package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.engine.Engine;
import eu.modernmt.engine.config.EngineConfig;
import eu.modernmt.engine.config.INIEngineConfigBuilder;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.rest.RESTServer;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by davide on 22/04/16.
 */
public class ClusterNodeMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option engine = Option.builder("e").longOpt("engine").hasArg().required().build();
            Option apiPort = Option.builder("a").longOpt("api-port").hasArg().type(Integer.class).required(false).build();
            Option clusterPorts = Option.builder("p").longOpt("cluster-ports").numberOfArgs(2).type(Integer.class).required().build();
            Option statusFile = Option.builder().longOpt("status-file").hasArg().required().build();
            Option verbosity = Option.builder("v").longOpt("verbosity").hasArg().type(Integer.class).required(false).build();

            Option member = Option.builder().longOpt("member").hasArg().required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(engine);
            cliOptions.addOption(apiPort);
            cliOptions.addOption(clusterPorts);
            cliOptions.addOption(statusFile);
            cliOptions.addOption(verbosity);
            cliOptions.addOption(member);
        }

        public final String engine;
        public final int apiPort;
        public final int controlPort;
        public final int dataPort;
        public final File statusFile;
        public final int verbosity;
        public final String member;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            this.engine = cli.getOptionValue("engine");
            String[] ports = cli.getOptionValues("cluster-ports");
            this.controlPort = Integer.parseInt(ports[0]);
            this.dataPort = Integer.parseInt(ports[1]);
            this.statusFile = new File(cli.getOptionValue("status-file"));

            String apiPort = cli.getOptionValue("api-port");
            this.apiPort = apiPort == null ? -1 : Integer.parseInt(apiPort);

            String verbosity = cli.getOptionValue("verbosity");
            this.verbosity = verbosity == null ? 2 : Integer.parseInt(verbosity);

            this.member = cli.getOptionValue("member");
        }
    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);
        Log4jConfiguration.setup(args.verbosity);

        FileStatusListener listener = new FileStatusListener(args);

        File engineConfigFile = Engine.getConfigFile(args.engine);
        EngineConfig engineConfig = new INIEngineConfigBuilder(engineConfigFile).build(args.engine);

        ModernMT.ClusterOptions options = new ModernMT.ClusterOptions();
        options.controlPort = args.controlPort;
        options.dataPort = args.dataPort;
        options.member = args.member;
        options.statusListener = listener;

        try {
            ModernMT.start(options, engineConfig);

            if (args.apiPort > 0) {
                RESTServer restServer = new RESTServer(args.apiPort);
                restServer.start();
            }

            listener.storeStatus(ClusterNode.Status.READY);
        } catch (Throwable e) {
            listener.onError();
            throw e;
        }
    }

    private static class FileStatusListener implements ClusterNode.StatusListener {

        private final File file;
        private final Properties status;

        public FileStatusListener(Args args) {
            this.file = args.statusFile;
            this.status = new Properties();
            status.setProperty("control_port", Integer.toString(args.controlPort));
            status.setProperty("data_port", Integer.toString(args.dataPort));
            if (args.apiPort > 0)
                status.setProperty("api_port", Integer.toString(args.apiPort));
        }

        @Override
        public void onStatusChanged(ClusterNode node, ClusterNode.Status currentStatus, ClusterNode.Status previousStatus) {
            if (currentStatus == ClusterNode.Status.READY)
                return; // Wait for REST Api to be ready

            storeStatus(currentStatus);
        }

        public void storeStatus(ClusterNode.Status status) {
            storeStatus(status.toString());
        }

        public void onError() {
            storeStatus("ERROR");
        }

        private void storeStatus(String status) {
            this.status.setProperty("status", status);

            FileOutputStream output = null;
            try {
                output = new FileOutputStream(file, false);
                this.status.store(output, null);
            } catch (IOException e) {
                // Nothing to do
            } finally {
                IOUtils.closeQuietly(output);
            }
        }
    }

}
