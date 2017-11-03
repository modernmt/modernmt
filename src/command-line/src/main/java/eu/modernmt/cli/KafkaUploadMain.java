package eu.modernmt.cli;

import eu.modernmt.cluster.kafka.KafkaChannel;
import eu.modernmt.cluster.kafka.KafkaDataManager;
import eu.modernmt.cluster.kafka.KafkaPacket;
import eu.modernmt.config.DataStreamConfig;
import eu.modernmt.data.DataManager;
import eu.modernmt.data.DataManagerException;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.CompactFileCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * Created by andrea on 19/10/17.
 */
public class KafkaUploadMain {


    private static final HashMap<String, InputFormat> FORMATS;

    //input formats
    static {
        FORMATS = new HashMap<>();
        FORMATS.put("tmx", new TMXInputFormat());
        FORMATS.put("parallel", new ParallelFileInputFormat());
        FORMATS.put("compact", new CompactInputFormat());
    }

    private interface InputFormat {
        MultilingualCorpus parse(Locale sourceLanguage, Locale targetLanguage, File[] files) throws ParseException;
    }

    private static class TMXInputFormat implements InputFormat {
        @Override
        public MultilingualCorpus parse(Locale sourceLanguage, Locale targetLanguage, File[] files) throws ParseException {
            if (files.length != 1)
                throw new ParseException("Invalid number of arguments: expected 1 file");
            return new TMXCorpus(files[0]);
        }
    }

    private static class CompactInputFormat implements InputFormat {
        @Override
        public MultilingualCorpus parse(Locale sourceLanguage, Locale targetLanguage, File[] files) throws ParseException {
            if (files.length != 1)
                throw new ParseException("Invalid number of arguments: expected 1 file");
            return new CompactFileCorpus(files[0]);
        }
    }

    private static class ParallelFileInputFormat implements InputFormat {
        @Override
        public MultilingualCorpus parse(Locale sourceLanguage, Locale targetLanguage, File[] files) throws ParseException {
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

        public final Locale sourceLanguage;
        public final Locale targetLanguage;
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

            sourceLanguage = cli.hasOption('s') ? Locale.forLanguageTag(cli.getOptionValue("s")) : null;
            targetLanguage = cli.hasOption('t') ? Locale.forLanguageTag(cli.getOptionValue("t")) : null;
            corpus = getCorpusInstance(cli, "input");
            memory = Long.parseLong(cli.getOptionValue("memory"));

            host = cli.hasOption("host") ? cli.getOptionValue("host") : null;
            port = cli.hasOption("port") ? Integer.parseInt(cli.getOptionValue("port")) : 0;
            name = cli.hasOption("name") ? cli.getOptionValue("name") : null;
        }
    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        DataStreamConfig defaultConfig = new DataStreamConfig();

        String host = args.host != null ? args.host : defaultConfig.getHost();
        int port = args.port != 0 ? args.port : defaultConfig.getPort();
        String name = args.name != null ? args.name : defaultConfig.getName();


        //get the KafkaChannel used to upload files in MMT
        String[] topicNames = KafkaDataManager.getDefaultTopicNames(name);
        KafkaChannel channel = new KafkaChannel(DataManager.MEMORY_UPLOAD_CHANNEL_ID, topicNames[DataManager.MEMORY_UPLOAD_CHANNEL_ID]);

        //get a valid KafkaProducer
        Properties producerProperties = KafkaDataManager.loadProperties("kafka-producer.properties", host, port);
        KafkaProducer<Integer, KafkaPacket> producer = new KafkaProducer<>(producerProperties);

        //for each entry in the corpus, use it to create a new KafkaPacket and write it into the kafka channel.
        MultilingualCorpus.MultilingualLineReader reader = null;
        KafkaPacket packet = null;
        try {
            reader = args.corpus.getContentReader();
            MultilingualCorpus.StringPair pair = reader.read();

            while (pair != null) {
                packet = KafkaPacket.createAddition(pair.language, args.memory, pair.source, pair.target, pair.timestamp);
                producer.send(new ProducerRecord<>(channel.getName(), 0, packet)).get();
                pair = reader.read();
            }
        } catch (IOException e) {
            throw new DataManagerException("Failed to read corpus for memory " + args.memory, e);
        } catch (InterruptedException e) {
            throw new DataManagerException("Interrupted upload for packet " + packet, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new DataManagerException("Unexpected exception while uploading packet " + packet, cause);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}