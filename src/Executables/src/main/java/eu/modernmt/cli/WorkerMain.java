package eu.modernmt.cli;

import eu.modernmt.engine.MMTWorker;
import eu.modernmt.engine.TranslationEngine;
import org.apache.commons.cli.*;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 17/12/15.
 */
public class WorkerMain {

    private static final Options cliOptions;

    static {
        Option engine = Option.builder("e").longOpt("engine").hasArg().required().build();
        Option master = Option.builder("m").longOpt("master").hasArg().required(false).build();
        Option clusterPorts = Option.builder("p").longOpt("cluster-ports").hasArgs().numberOfArgs(2).type(Integer.class).required().build();

        cliOptions = new Options();
        cliOptions.addOption(engine);
        cliOptions.addOption(master);
        cliOptions.addOption(clusterPorts);
    }

    public static void main(String[] args) throws Throwable {
        CommandLineParser parser = new DefaultParser();
        CommandLine cli = parser.parse(cliOptions, args);

        TranslationEngine engine = TranslationEngine.get(cli.getOptionValue("engine"));

        String master = cli.hasOption("master") ? cli.getOptionValue("master") : null;
        int threads = engine.getDecoderThreads();
        String[] sPorts = cli.getOptionValues("cluster-ports");
        int[] ports = new int[]{Integer.parseInt(sPorts[0]), Integer.parseInt(sPorts[1])};
        MMTWorker worker = new MMTWorker(engine, master, ports, threads);

        Runtime.getRuntime().addShutdownHook(new ShutdownHook(worker));
        worker.start();
    }

    public static class ShutdownHook extends Thread {

        private MMTWorker worker;

        public ShutdownHook(MMTWorker worker) {
            this.worker = worker;
        }

        @Override
        public void run() {
            try {
                worker.shutdown();
                worker.awaitTermination(1, TimeUnit.DAYS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
