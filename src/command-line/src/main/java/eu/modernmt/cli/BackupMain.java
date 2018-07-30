package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.cluster.BackupDaemon;
import eu.modernmt.config.ConfigException;
import eu.modernmt.config.NodeConfig;
import eu.modernmt.config.xml.XMLConfigBuilder;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;

import java.io.File;

public class BackupMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option configFile = Option.builder("c").longOpt("config").hasArg().required().build();
            Option backup = Option.builder("b").longOpt("backup").hasArg().required().build();
            Option limit = Option.builder("l").longOpt("limit").hasArg().required().build();
            Option time = Option.builder("t").longOpt("time").hasArg().required().build();
            Option logFile = Option.builder().longOpt("log-file").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(configFile);
            cliOptions.addOption(backup);
            cliOptions.addOption(limit);
            cliOptions.addOption(logFile);
            cliOptions.addOption(time);
        }

        public final File backupFolder;
        public final File logFile;
        public final int limit;
        public final NodeConfig config;
        public long time;

        public Args(String[] args) throws ParseException, ConfigException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            this.backupFolder = new File(cli.getOptionValue("backup"));
            this.logFile = new File(cli.getOptionValue("log-file"));
            this.limit = Integer.parseInt(cli.getOptionValue("limit"));
            this.config = XMLConfigBuilder.build(new File(cli.getOptionValue("config")));
            this.time = Long.parseLong(cli.getOptionValue("time"));
        }
    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);
        Log4jConfiguration.setup(args.logFile, Level.INFO);

        BackupDaemon daemon = new BackupDaemon(args.backupFolder, args.limit);
        daemon.runForever(args.config, args.time * 1000L);
    }

}