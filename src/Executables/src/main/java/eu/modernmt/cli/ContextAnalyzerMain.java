//package eu.modernmt.cli;
//
//import eu.modernmt.context.ContextAnalyzer;
//import eu.modernmt.model.FileCorpus;
//import org.apache.commons.cli.*;
//
//import java.io.File;
//
///**
// * Created by davide on 17/12/15.
// */
//public class ContextAnalyzerMain {
//
//    private static final Options cliOptions;
//
//    static {
//        Option index = Option.builder("i").longOpt("index-path").hasArg().required().build();
//        Option corpora = Option.builder("c").longOpt("corpora").hasArg().required().build();
//        Option language = Option.builder("l").longOpt("lang").hasArg().required(false).build();
//
//        cliOptions = new Options();
//        cliOptions.addOption(index);
//        cliOptions.addOption(corpora);
//        cliOptions.addOption(language);
//    }
//
//    public static void main(String[] args) throws Throwable {
//        CommandLineParser parser = new DefaultParser();
//        CommandLine cmd = parser.parse(cliOptions, args);
//
//        File indexPath = new File(cmd.getOptionValue('i'));
//        File corpora = new File(cmd.getOptionValue('c'));
//        String lang = cmd.hasOption('l') ? cmd.getOptionValue('l') : null;
//
//        ContextAnalyzer contextAnalyzer = new ContextAnalyzer(indexPath);
//        contextAnalyzer.rebuild(FileCorpus.list(corpora, lang));
//    }
//}
