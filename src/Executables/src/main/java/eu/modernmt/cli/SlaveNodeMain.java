package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.engine.SlaveNode;
import eu.modernmt.engine.TranslationEngine;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 17/12/15.
 */
public class SlaveNodeMain {

    private static final Options cliOptions;

    static {
        Option engine = Option.builder("e").longOpt("engine").hasArg().required().build();
        Option masterHost = Option.builder().longOpt("master-host").hasArg().required(false).build();
        Option masterUser = Option.builder().longOpt("master-user").hasArg().required(false).build();
        Option masterPasswd = Option.builder().longOpt("master-passwd").hasArg().required(false).build();
        Option masterPem = Option.builder().longOpt("master-pem").hasArg().required(false).build();
        Option clusterPorts = Option.builder("p").longOpt("cluster-ports").hasArgs().numberOfArgs(2).type(Integer.class).required().build();
        Option statusFile = Option.builder().longOpt("status-file").hasArg().required().build();

        cliOptions = new Options();
        cliOptions.addOption(engine);
        cliOptions.addOption(masterHost);
        cliOptions.addOption(masterUser);
        cliOptions.addOption(masterPasswd);
        cliOptions.addOption(masterPem);
        cliOptions.addOption(clusterPorts);
        cliOptions.addOption(statusFile);
    }

    public static void main(String[] args) throws Throwable {
        Log4jConfiguration.setup(2);

        CommandLineParser parser = new DefaultParser();
        CommandLine cli = parser.parse(cliOptions, args);

        boolean ready = false;

        try {
            TranslationEngine engine = new TranslationEngine(cli.getOptionValue("engine"));

            SlaveNode.MasterHost master = null;

            if (cli.hasOption("master-host")) {
                master = new SlaveNode.MasterHost();
                master.host = cli.getOptionValue("master-host");
                master.user = cli.getOptionValue("master-user");
                master.password = cli.getOptionValue("master-passwd");
                master.pem = cli.hasOption("master-pem") ? new File(cli.getOptionValue("master-pem")) : null;
            }

            String[] sPorts = cli.getOptionValues("cluster-ports");
            int[] ports = new int[]{Integer.parseInt(sPorts[0]), Integer.parseInt(sPorts[1])};
            SlaveNode worker = new SlaveNode(engine, master, ports);
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(worker));
            worker.start();
            worker.awaitInitialization();

            ready = worker.isActive();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            File statusFile = new File(cli.getOptionValue("status-file"));
            FileUtils.write(statusFile, ready ? "ready" : "error", false);
        }
    }

    public static class ShutdownHook extends Thread {

        private SlaveNode worker;

        public ShutdownHook(SlaveNode worker) {
            this.worker = worker;
        }

        @Override
        public void run() {
            try {
                worker.shutdown();
                worker.awaitTermination(1, TimeUnit.DAYS);
            } catch (Exception e) {
                // Nothing to do
            }
        }

    }
}
