package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.core.Engine;
import eu.modernmt.core.cluster.ClusterNode;
import eu.modernmt.core.cluster.DirectorySynchronizer;
import eu.modernmt.core.cluster.RSyncSynchronizer;
import eu.modernmt.core.config.EngineConfig;
import eu.modernmt.core.config.INIEngineConfigBuilder;
import eu.modernmt.core.facade.ModernMT;
import eu.modernmt.rest.RESTServer;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 22/04/16.
 */
public class ClusterNodeMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option engine = Option.builder("e").longOpt("engine").hasArg().required().build();
            Option apiPort = Option.builder("a").longOpt("api-port").hasArg().type(Integer.class).required(false).build();
            Option clusterPort = Option.builder("p").longOpt("cluster-port").hasArg().type(Integer.class).required().build();
            Option statusFile = Option.builder().longOpt("status-file").hasArg().required().build();
            Option verbosity = Option.builder("v").longOpt("verbosity").hasArg().type(Integer.class).required(false).build();

            Option memberHost = Option.builder().longOpt("node-host").hasArg().required(false).build();
            Option memberUser = Option.builder().longOpt("node-user").hasArg().required(false).build();
            Option memberPasswd = Option.builder().longOpt("node-passwd").hasArg().required(false).build();
            Option memberPem = Option.builder().longOpt("node-pem").hasArg().required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(engine);
            cliOptions.addOption(apiPort);
            cliOptions.addOption(clusterPort);
            cliOptions.addOption(statusFile);
            cliOptions.addOption(verbosity);
            cliOptions.addOption(memberHost);
            cliOptions.addOption(memberUser);
            cliOptions.addOption(memberPasswd);
            cliOptions.addOption(memberPem);
        }

        public final String engine;
        public final int apiPort;
        public final int clusterPort;
        public final File statusFile;
        public final int verbosity;
        public final String memberHost;
        public final String memberUser;
        public final String memberPassword;
        public final File memberPem;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            this.engine = cli.getOptionValue("engine");
            this.clusterPort = Integer.parseInt(cli.getOptionValue("cluster-port"));
            this.statusFile = new File(cli.getOptionValue("status-file"));

            String apiPort = cli.getOptionValue("api-port");
            this.apiPort = apiPort == null ? -1 : Integer.parseInt(apiPort);

            String verbosity = cli.getOptionValue("verbosity");
            this.verbosity = verbosity == null ? 2 : Integer.parseInt(verbosity);

            this.memberHost = cli.getOptionValue("node-host");
            if (this.memberHost != null) {
                this.memberUser = cli.getOptionValue("node-user");

                String memberPem = cli.getOptionValue("node-pem");
                this.memberPem = memberPem == null ? null : new File(memberPem);
                this.memberPassword = this.memberPem == null ? cli.getOptionValue("node-passwd") : null;
            } else {
                this.memberUser = null;
                this.memberPassword = null;
                this.memberPem = null;
            }
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);
        Log4jConfiguration.setup(args.verbosity);

        final Logger mainLogger = LogManager.getLogger(ClusterNodeMain.class);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            mainLogger.fatal("Unexpected exception thrown by thread [" + t.getName() + "]", e);
            e.printStackTrace();
        });

        StatusManager status = new StatusManager(args.statusFile);

        RESTServer restServer = null;
        ClusterNode node = null;

        boolean ready = false;

        try {
            node = new ClusterNode(args.clusterPort);
            ModernMT.setLocalNode(node);

            if (args.memberHost != null)
                node.joinCluster(args.memberHost, 30, TimeUnit.SECONDS);
            else
                node.startCluster();

            if (args.apiPort > 0) {
                restServer = new RESTServer(args.apiPort);
                restServer.start();
            }

            status.onClusterJoined();

            if (args.memberHost != null) {
                InetAddress host = InetAddress.getByName(args.memberHost);
                File localPath = Engine.getRootPath(args.engine);
                String remotePath = node.getMemberModelPath(args.memberHost);

                if (remotePath == null)
                    throw new ParseException("Invalid remote host: " + args.memberHost);

                DirectorySynchronizer synchronizer;
                if (args.memberPem != null)
                    synchronizer = new RSyncSynchronizer(host, args.memberPem, localPath, remotePath);
                else
                    synchronizer = new RSyncSynchronizer(host, args.memberUser, args.memberPassword, localPath, remotePath);

                synchronizer.synchronize();
                status.onModelSynchronized();
            }

            EngineConfig config = new INIEngineConfigBuilder(Engine.getConfigFile(args.engine)).build(args.engine);
            node.bootstrap(config);

            status.onModelLoaded();

            ready = true;
        } catch (Throwable e) {
            status.onError();
            throw e;
        } finally {
            if (ready) {
                Runtime.getRuntime().addShutdownHook(new ShutdownHook(restServer, node));
            } else {
                shutdown(node);
                shutdown(restServer);
            }
        }
    }

    public static class ShutdownHook extends Thread {

        private final RESTServer restServer;
        private final ClusterNode node;

        public ShutdownHook(RESTServer restServer, ClusterNode node) {
            this.restServer = restServer;
            this.node = node;
        }

        @Override
        public void run() {
            shutdown(restServer);
            shutdown(node);

            await(restServer);
            await(node);
        }

    }

    private static class StatusManager {

        private final File file;

        public StatusManager(File file) {
            this.file = file;
        }

        public void onClusterJoined() {
            write("JOINED");
        }

        public void onModelSynchronized() {
            write("SYNCHRONIZED");
        }

        public void onModelLoaded() {
            write("READY");
        }

        public void onError() {
            write("ERROR");
        }

        private void write(String status) {
            try {
                FileUtils.write(file, status, "UTF-8", false);
            } catch (IOException e) {
                // Nothing to do
            }
        }
    }

    private static void shutdown(RESTServer restServer) {
        try {
            if (restServer != null)
                restServer.stop();
        } catch (Throwable e) {
            // Ignore
        }
    }

    private static void shutdown(ClusterNode node) {
        try {
            if (node != null)
                node.shutdown();
        } catch (Throwable e) {
            // Ignore
        }
    }

    private static void await(RESTServer restServer) {
        try {
            if (restServer != null)
                restServer.join();
        } catch (Throwable e) {
            // Ignore
        }
    }

    private static void await(ClusterNode node) {
        try {
            if (node != null)
                node.awaitTermination(1, TimeUnit.DAYS);
        } catch (Throwable e) {
            // Ignore
        }
    }

}
