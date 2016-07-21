package eu.modernmt.cli;

import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.impl.ebay4cb.ParallelEbay4CBFile;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.impl.properties.ParallelPropertiesCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import eu.modernmt.model.corpus.impl.xliff.XLIFFCorpus;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by davide on 04/07/16.
 */
public class ConvertMain {

    private static final HashMap<String, InputFormat> FORMATS;

    static {
        FORMATS = new HashMap<>();
        FORMATS.put("tmx", new TMXInputFormat());
        FORMATS.put("4cb", new FourCBInputFormat());
        FORMATS.put("parallel", new ParallelFileInputFormat());
        FORMATS.put("properties", new ParallelPropertiesInputFormat());
        FORMATS.put("xliff", new XLIFFInputFormat());
    }

    private interface InputFormat {

        BilingualCorpus parse(Locale sourceLanguage, Locale targetLanguage, File[] files) throws ParseException;

    }

    private static class TMXInputFormat implements InputFormat {

        @Override
        public BilingualCorpus parse(Locale sourceLanguage, Locale targetLanguage, File[] files) throws ParseException {
            if (files.length != 1)
                throw new ParseException("Invalid number of arguments: expected 1 file");
            return new TMXCorpus(files[0], sourceLanguage, targetLanguage);
        }

    }

    private static class FourCBInputFormat implements InputFormat {

        @Override
        public BilingualCorpus parse(Locale sourceLanguage, Locale targetLanguage, File[] files) throws ParseException {
            if (files.length != 2)
                throw new ParseException("Invalid number of arguments: expected 2 files");
            return new ParallelEbay4CBFile(sourceLanguage, files[0], targetLanguage, files[1]);
        }

    }

    private static class ParallelFileInputFormat implements InputFormat {

        @Override
        public BilingualCorpus parse(Locale sourceLanguage, Locale targetLanguage, File[] files) throws ParseException {
            if (files.length != 2)
                throw new ParseException("Invalid number of arguments: expected 2 files");
            return new ParallelFileCorpus(sourceLanguage, files[0], targetLanguage, files[1]);
        }

    }

    private static class ParallelPropertiesInputFormat implements InputFormat {

        @Override
        public BilingualCorpus parse(Locale sourceLanguage, Locale targetLanguage, File[] files) throws ParseException {
            if (files.length != 2)
                throw new ParseException("Invalid number of arguments: expected 2 files");
            return new ParallelPropertiesCorpus(sourceLanguage, files[0], targetLanguage, files[1]);
        }

    }

    private static class XLIFFInputFormat implements InputFormat {

        @Override
        public BilingualCorpus parse(Locale sourceLanguage, Locale targetLanguage, File[] files) throws ParseException {
            if (files.length != 1)
                throw new ParseException("Invalid number of arguments: expected 1 file");
            return new XLIFFCorpus(files[0], sourceLanguage, targetLanguage);
        }

    }

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option input = Option.builder().longOpt("input").hasArgs().required().build();
            Option inputFormat = Option.builder().longOpt("input-format").hasArg().required().build();
            Option output = Option.builder().longOpt("output").hasArgs().required().build();
            Option outputFormat = Option.builder().longOpt("output-format").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(input);
            cliOptions.addOption(inputFormat);
            cliOptions.addOption(output);
            cliOptions.addOption(outputFormat);
        }

        public final BilingualCorpus input;
        public final BilingualCorpus output;
        public final Locale sourceLanguage;
        public final Locale targetLanguage;

        private BilingualCorpus getCorpusInstance(CommandLine cli, String prefix) throws ParseException {
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

            sourceLanguage = Locale.forLanguageTag(cli.getOptionValue('s'));
            targetLanguage = Locale.forLanguageTag(cli.getOptionValue('t'));
            input = getCorpusInstance(cli, "input");
            output = getCorpusInstance(cli, "output");
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        BilingualCorpus.BilingualLineReader reader = null;
        BilingualCorpus.BilingualLineWriter writer = null;

        try {
            reader = args.input.getContentReader();
            writer = args.output.getContentWriter(false);

            BilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                writer.write(pair);
            }
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

}
