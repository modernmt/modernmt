package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.io.IOCorporaUtils;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.CompactFileCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by davide on 04/07/16.
 */
public class ConvertMain {

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

        public final MultilingualCorpus input;
        public final MultilingualCorpus output;
        public final Locale sourceLanguage;
        public final Locale targetLanguage;

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

            /*source and target language may be null if conversion is from tmx to compact or vice versa*/
            sourceLanguage = cli.hasOption('s') ? Locale.forLanguageTag(cli.getOptionValue("s")) : null;
            targetLanguage = cli.hasOption('t') ? Locale.forLanguageTag(cli.getOptionValue("t")) : null;

            input = getCorpusInstance(cli, "input");
            output = getCorpusInstance(cli, "output");

        }
    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);
        Args args = new Args(_args);

        IOCorporaUtils.copy(args.input, args.output);
    }
}
