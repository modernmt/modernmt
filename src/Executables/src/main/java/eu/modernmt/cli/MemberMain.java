package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.core.Engine;
import eu.modernmt.core.cluster.Member;
import eu.modernmt.core.config.EngineConfig;
import eu.modernmt.core.config.INIEngineConfigBuilder;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 22/04/16.
 */
public class MemberMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option engine = Option.builder("e").longOpt("engine").hasArg().required().build();
            Option clusterPort = Option.builder("p").longOpt("cluster-port").hasArg().type(Integer.class).required().build();
            Option statusFile = Option.builder().longOpt("status-file").hasArg().required().build();
            Option verbosity = Option.builder("v").longOpt("verbosity").hasArg().type(Integer.class).required(false).build();

            Option memberHost = Option.builder().longOpt("member-host").hasArg().required(false).build();
            Option memberUser = Option.builder().longOpt("member-user").hasArg().required(false).build();
            Option memberPasswd = Option.builder().longOpt("member-passwd").hasArg().required(false).build();
            Option memberPem = Option.builder().longOpt("member-pem").hasArg().required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(engine);
            cliOptions.addOption(clusterPort);
            cliOptions.addOption(statusFile);
            cliOptions.addOption(verbosity);
            cliOptions.addOption(memberHost);
            cliOptions.addOption(memberUser);
            cliOptions.addOption(memberPasswd);
            cliOptions.addOption(memberPem);
        }

        public final String engine;
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

            String verbosity = cli.getOptionValue("verbosity");
            this.verbosity = verbosity == null ? 2 : Integer.parseInt(verbosity);

            this.memberHost = cli.getOptionValue("member-host");
            if (this.memberHost != null) {
                this.memberUser = cli.getOptionValue("member-user");

                String memberPem = cli.getOptionValue("member-pem");
                this.memberPem = memberPem == null ? null : new File(memberPem);
                this.memberPassword = this.memberPem == null ? cli.getOptionValue("member-passwd") : null;
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
        StatusManager status = new StatusManager(args.statusFile);

        Member member = null;
        boolean ready = false;
        try {
            File engineINI = Engine.getConfigFile(args.engine);
            EngineConfig config = new INIEngineConfigBuilder(engineINI).build();

            member = new Member(args.clusterPort);

            if (args.memberHost != null)
                member.joinCluster(args.memberHost, 30, TimeUnit.SECONDS);
            else
                member.startCluster();

            status.onClusterJoined();

            if (args.memberHost != null) {
                // TODO:
//                InetAddress host = InetAddress.getByName(args.memberHost);
//
//                DirectorySynchronizer synchronizer;
//                if (args.memberPem != null)
//                    synchronizer = new RSyncSynchronizer(host, args.memberPem, )
            }

            member.bootstrap(config);
            status.onModelLoaded();

            ready = true;
        } catch (Throwable e) {
            status.onError();
            throw e;
        } finally {
            if (ready) {
                Runtime.getRuntime().addShutdownHook(new ShutdownHook(member));
            } else {
                if (member != null)
                    member.shutdown();
            }
        }
    }

    public static class ShutdownHook extends Thread {

        private Member member;

        public ShutdownHook(Member member) {
            this.member = member;
        }

        @Override
        public void run() {
            try {
                member.shutdown();
                member.awaitTermination(1, TimeUnit.DAYS);
            } catch (Exception e) {
                // Nothing to do
            }
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
                FileUtils.write(file, status, "UTF-8", true);
            } catch (IOException e) {
                // Nothing to do
            }
        }
    }

}
