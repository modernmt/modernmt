package eu.modernmt.cli;

import eu.modernmt.cleaning.CorporaCleaning;
import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.cluster.kafka.KafkaDataManager;
import eu.modernmt.config.DataStreamConfig;
import eu.modernmt.data.DataManager;
import eu.modernmt.data.DataManagerException;
import eu.modernmt.data.HostUnreachableException;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ImportJob;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.CompactFileCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.JSONSerializer;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by andrea on 19/10/17.
 */
public class CorpusUploadMain {


    private static final HashMap<String, InputFormat> FORMATS;

    //input formats
    static {
        FORMATS = new HashMap<>();
        FORMATS.put("tmx", new TMXInputFormat());
        FORMATS.put("parallel", new ParallelFileInputFormat());
        FORMATS.put("compact", new CompactInputFormat());
    }

    private interface InputFormat {
        MultilingualCorpus parse(Language sourceLanguage, Language targetLanguage, File[] files) throws ParseException;
    }

    private static class TMXInputFormat implements InputFormat {
        @Override
        public MultilingualCorpus parse(Language sourceLanguage, Language targetLanguage, File[] files) throws ParseException {
            if (files.length != 1)
                throw new ParseException("Invalid number of arguments: expected 1 file");
            return new TMXCorpus(files[0]);
        }
    }

    private static class CompactInputFormat implements InputFormat {
        @Override
        public MultilingualCorpus parse(Language sourceLanguage, Language targetLanguage, File[] files) throws ParseException {
            if (files.length != 1)
                throw new ParseException("Invalid number of arguments: expected 1 file");
            return new CompactFileCorpus(files[0]);
        }
    }

    private static class ParallelFileInputFormat implements InputFormat {
        @Override
        public MultilingualCorpus parse(Language sourceLanguage, Language targetLanguage, File[] files) throws ParseException {
            if (files.length != 2)
                throw new ParseException("Invalid number of arguments: expected 2 files");
            if (sourceLanguage == null)
                throw new ParseException("Invalid input: source language is mandatory for parallel corpora");
            if (targetLanguage == null)
                throw new ParseException("Invalid input: target language is mandatory for parallel corpora");

            return new ParallelFileCorpus(new LanguagePair(sourceLanguage, targetLanguage), files[0], files[1]);
        }
    }


    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().build();
            Option targetLanguage = Option.builder("t").hasArg().build();
            Option input = Option.builder().longOpt("input").hasArgs().required().build();
            Option inputFormat = Option.builder().longOpt("input-format").hasArg().required().build();
            Option memory = Option.builder().longOpt("memory").hasArgs().required().build();
            Option host = Option.builder().longOpt("host").hasArgs().build();
            Option port = Option.builder().longOpt("port").hasArgs().build();
            Option name = Option.builder().longOpt("name").hasArgs().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(input);
            cliOptions.addOption(inputFormat);
            cliOptions.addOption(memory);
            cliOptions.addOption(host);
            cliOptions.addOption(port);
            cliOptions.addOption(name);
        }

        public final Language sourceLanguage;
        public final Language targetLanguage;
        public final MultilingualCorpus corpus;
        public final long memory;
        public final String host;
        public final int port;
        public final String name;

        private MultilingualCorpus getCorpusInstance(CommandLine cli, String prefix) throws ParseException {
            String formatName = cli.getOptionValue(prefix + "-format");
            InputFormat format = FORMATS.get(formatName.toLowerCase());

            if (format == null)
                throw new ParseException("Invalid format '" + formatName + "', must be one of " + FORMATS.keySet());

            String[] arg = cli.getOptionValues(prefix);
            File[] files = new File[arg.length];
            for (int i = 0; i < arg.length; i++)
                files[i] = new File(arg[i]);

            return format.parse(sourceLanguage, targetLanguage, files);
        }

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            sourceLanguage = cli.hasOption('s') ? Language.fromString(cli.getOptionValue("s")) : null;
            targetLanguage = cli.hasOption('t') ? Language.fromString(cli.getOptionValue("t")) : null;
            corpus = getCorpusInstance(cli, "input");
            memory = Long.parseLong(cli.getOptionValue("memory"));

            host = cli.hasOption("host") ? cli.getOptionValue("host") : null;
            port = cli.hasOption("port") ? Integer.parseInt(cli.getOptionValue("port")) : 0;
            name = cli.hasOption("name") ? cli.getOptionValue("name") : null;
        }
    }

    /**
     * A remote memory facade models an access point to the memories of a remote ModernMT instance.
     * It can be used to upload a corpus to a remote ModernMT.
     */
    private static class RemoteMemoryFacade {

        private KafkaDataManager dataManager;

        public RemoteMemoryFacade(String host, int port, String name) throws HostUnreachableException {

            DataStreamConfig config = new DataStreamConfig();

            if (host != null)
                config.setHost(host);
            if (port != 0)
                config.setPort(port);
            if (name != null)
                config.setName(name);

            config.setEmbedded(false);
            config.setEnabled(true);

            this.dataManager = new KafkaDataManager(null, null, config);
            this.dataManager.connect(30, TimeUnit.SECONDS, false, true);
        }

        public ImportJob add(long memoryId, MultilingualCorpus corpus) throws PersistenceException, DataManagerException {
            corpus = CorporaCleaning.wrap(corpus);
            return dataManager.upload(memoryId, corpus, DataManager.MEMORY_UPLOAD_CHANNEL_ID);
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.ERROR);

        Args args = new Args(_args);
        RemoteMemoryFacade memory = new RemoteMemoryFacade(args.host, args.port, args.name);
        ImportJob job = memory.add(args.memory, args.corpus);
        System.out.println(JSONSerializer.toJSON(job, ImportJob.class));
    }
}