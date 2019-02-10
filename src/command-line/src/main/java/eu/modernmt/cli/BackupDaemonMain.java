package eu.modernmt.cli;

import eu.modernmt.backup.BackupDaemon;
import eu.modernmt.backup.FileLimitRetentionPolicy;
import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.config.ConfigException;
import eu.modernmt.config.NodeConfig;
import eu.modernmt.config.xml.XMLConfigBuilder;
import eu.modernmt.engine.Engine;
import eu.modernmt.io.FileConst;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.File;

public class BackupDaemonMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option engine = Option.builder("e").longOpt("engine").hasArg().required().build();
            Option limit = Option.builder("i").longOpt("interval").hasArg().required().build();
            Option time = Option.builder("l").longOpt("limit").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(engine);
            cliOptions.addOption(limit);
            cliOptions.addOption(time);
        }

        public final String engine;
        public final NodeConfig config;
        public final int limit;
        public final long interval;

        public Args(String[] args) throws ParseException, ConfigException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            this.engine = cli.getOptionValue("engine");
            this.config = XMLConfigBuilder.build(Engine.getConfigFile(this.engine));
            this.config.getEngineConfig().setName(this.engine);
            this.interval = Long.parseLong(cli.getOptionValue("interval")) * 1000L;
            this.limit = Integer.parseInt(cli.getOptionValue("limit"));
        }
    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        File logFolder = FileConst.getLogsPath(args.engine);
        FileUtils.forceMkdir(logFolder);

        Log4jConfiguration.setup(new File(logFolder, "backup.log"), Level.INFO);

        FileLimitRetentionPolicy policy = new FileLimitRetentionPolicy(args.limit);

        try (BackupDaemon daemon = new BackupDaemon(args.config, policy, args.interval)) {
            daemon.runForever();
        }

        LogManager.shutdown();
    }

}
