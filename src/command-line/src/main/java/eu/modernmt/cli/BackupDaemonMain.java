package eu.modernmt.cli;

import eu.modernmt.backup.BackupDaemon;
import eu.modernmt.backup.FileLimitRetentionPolicy;
import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.config.ConfigException;
import eu.modernmt.config.NodeConfig;
import eu.modernmt.config.xml.XMLConfigBuilder;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.Engine;
import eu.modernmt.io.RuntimeIOException;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class BackupDaemonMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option engine = Option.builder("e").longOpt("engine").hasArg().required().build();
            Option limit = Option.builder("l").longOpt("limit").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(engine);
            cliOptions.addOption(limit);
        }

        public final String engine;
        public final NodeConfig config;
        public final int limit;

        public Args(String[] args) throws ParseException, ConfigException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            this.engine = cli.getOptionValue("engine");
            this.config = XMLConfigBuilder.build(Engine.getConfigFile(this.engine));
            this.config.getEngineConfig().setName(this.engine);
            this.limit = Integer.parseInt(cli.getOptionValue("limit"));
        }
    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);
        Log4jConfiguration.setup(Level.INFO);

        FileLimitRetentionPolicy policy = new FileLimitRetentionPolicy(args.limit);
        BackupDaemonMain daemon = new BackupDaemonMain(new BackupDaemon(args.config, policy));
        daemon.runForever();
    }

    private enum Command {
        QUIT, BACKUP, STATS
    }

    private final Logger logger = LogManager.getLogger(BackupDaemonMain.class);
    private final BackupDaemon daemon;
    private final SynchronousQueue<Command> commands = new SynchronousQueue<>();

    private BackupDaemonMain(BackupDaemon daemon) {
        this.daemon = daemon;
    }

    private void interrupt() {
        logger.info("Interrupt signal received, sending shutdown signal");
        commands.add(Command.QUIT);
    }

    private Command poll() {
        try {
            Command result = commands.poll(10, TimeUnit.MINUTES);
            if (result == null)
                return Command.STATS;
            return result;
        } catch (InterruptedException e) {
            return Command.QUIT;
        }
    }

    private void runForever() throws IOException, BootstrapException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        Runtime.getRuntime().addShutdownHook(new Thread(this::interrupt));
        new CommandReader(in, commands).start();

        daemon.start();

        try {
            while (true) {
                switch (poll()) {
                    case QUIT:
                        logger.info("Stopping backup daemon");
                        return;
                    case BACKUP:
                        daemon.backup();
                        break;
                    case STATS:
                        logger.info("Current engine channels status: " + daemon.getChannelsPositions());
                }
            }
        } finally {
            close();
        }
    }

    private void close() {
        try {
            daemon.close();
        } catch (Throwable e) {
            logger.error("Failed to stop engine", e);
        } finally {
            LogManager.shutdown();
        }
    }

    private static class CommandReader extends Thread {

        private final BufferedReader in;
        private final SynchronousQueue<Command> out;

        private CommandReader(BufferedReader in, SynchronousQueue<Command> out) {
            this.in = in;
            this.out = out;
            this.setDaemon(true);
        }

        private Command readCommand() {
            String line;
            try {
                line = in.readLine();

                if (line == null)
                    return null;
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }

            line = line.trim().toUpperCase();
            try {
                return Command.valueOf(line);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown command received: '" + line + "'");
            }
        }

        @Override
        public void run() {
            Command command;
            while ((command = readCommand()) != null) {
                try {
                    out.put(command);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

}
