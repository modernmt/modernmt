package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.config.*;
import eu.modernmt.config.xml.XMLConfigBuilder;
import eu.modernmt.engine.Engine;
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
            Option statusFile = Option.builder().longOpt("status-file").hasArg().required().build();
            Option logsFolder = Option.builder().longOpt("logs").hasArg().required().build();

            Option apiPort = Option.builder("a").longOpt("api-port").hasArg().type(Integer.class).required(false).build();
            Option clusterPorts = Option.builder("p").longOpt("cluster-ports").numberOfArgs(2).type(Integer.class).required(false).build();
            Option datastreamPort = Option.builder().longOpt("datastream-port").hasArg().required(false).build();
            Option member = Option.builder().longOpt("member").hasArg().required(false).build();

            Option verbosity = Option.builder("v").longOpt("verbosity").hasArg().type(Integer.class).required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(engine);
            cliOptions.addOption(apiPort);
            cliOptions.addOption(clusterPorts);
            cliOptions.addOption(statusFile);
            cliOptions.addOption(verbosity);
            cliOptions.addOption(member);
            cliOptions.addOption(logsFolder);
            cliOptions.addOption(datastreamPort);
        }

        public final String engine;
        public final File statusFile;
        public final File logsFolder;
        public final int verbosity;
        public final NodeConfig config;

        public Args(String[] args) throws ParseException, ConfigException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            this.engine = cli.getOptionValue("engine");
            this.statusFile = new File(cli.getOptionValue("status-file"));
            this.logsFolder = new File(cli.getOptionValue("logs"));

            String verbosity = cli.getOptionValue("verbosity");
            this.verbosity = verbosity == null ? 1 : Integer.parseInt(verbosity);

            this.config = XMLConfigBuilder.build(Engine.getConfigFile(this.engine));
            this.config.getEngineConfig().setName(this.engine);

            String[] ports = cli.getOptionValues("cluster-ports");
            if (ports != null && ports.length > 1) {
                NetworkConfig netConfig = this.config.getNetworkConfig();
                netConfig.setPort(Integer.parseInt(ports[0]));
                netConfig.setDataPort(Integer.parseInt(ports[1]));
            }

            String apiPort = cli.getOptionValue("api-port");
            if (apiPort != null) {
                ApiConfig apiConfig = this.config.getNetworkConfig().getApiConfig();
                apiConfig.setPort(Integer.parseInt(apiPort));
            }

            String member = cli.getOptionValue("member");
            if (member != null) {
                String[] parts = member.split(":");

                JoinConfig joinConfig = this.config.getNetworkConfig().getJoinConfig();

                JoinConfig.Member[] members = new JoinConfig.Member[1];
                members[0] = new JoinConfig.Member(parts[0], Integer.parseInt(parts[1]), 0);

                joinConfig.setMembers(members);
            }

            String datastreamPort = cli.getOptionValue("datastream-port");
            if (datastreamPort != null)
                this.config.getDataStreamConfig().setPort(Integer.parseInt(datastreamPort));
        }
    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);
        Log4jConfiguration.setup(args.verbosity, args.logsFolder);


        FileStatusListener listener = new FileStatusListener(args.statusFile, args.config);

        try {
            ModernMT.start(args.config, listener);

            ApiConfig apiConfig = args.config.getNetworkConfig().getApiConfig();

            if (apiConfig.isEnabled()) {
                RESTServer restServer = new RESTServer(apiConfig.getPort());
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

        public FileStatusListener(File file, NodeConfig config) {
            this.file = file;

            NetworkConfig netConfig = config.getNetworkConfig();
            ApiConfig apiConfig = netConfig.getApiConfig();

            this.status = new Properties();
            status.setProperty("control_port", Integer.toString(netConfig.getPort()));
            status.setProperty("data_port", Integer.toString(netConfig.getDataPort()));

            if (apiConfig.isEnabled())
                status.setProperty("api_port", Integer.toString(apiConfig.getPort()));
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
