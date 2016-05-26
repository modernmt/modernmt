package eu.modernmt.cli;

import eu.modernmt.cli.init.Submodules;
import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.core.Engine;
import eu.modernmt.core.cluster.ClusterNode;
import eu.modernmt.core.cluster.storage.DirectorySynchronizer;
import eu.modernmt.core.cluster.storage.StorageService;
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

    static {
        Submodules.link();
    }

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
            node = new ClusterNode(args.controlPort, args.dataPort);
            ModernMT.setLocalNode(node);

            if (args.member != null)
                node.joinCluster(args.member, 30, TimeUnit.SECONDS);
            else
                node.startCluster();

            status.onClusterJoined();

            if (args.member != null) {
                InetAddress host = InetAddress.getByName(args.member);
                File localPath = Engine.getRootPath(args.engine);

                StorageService storage = StorageService.getInstance();
                DirectorySynchronizer synchronizer = storage.getDirectorySynchronizer();
                synchronizer.synchronize(host, args.dataPort, localPath);

                status.onModelSynchronized();
            }

            EngineConfig config = new INIEngineConfigBuilder(Engine.getConfigFile(args.engine)).build(args.engine);
            node.bootstrap(config);

            status.onModelLoaded();

            if (args.apiPort > 0) {
                restServer = new RESTServer(args.apiPort);
                restServer.start();
            }

            status.onSystemReady();

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
            write("LOADED");
        }

        public void onSystemReady() {
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
