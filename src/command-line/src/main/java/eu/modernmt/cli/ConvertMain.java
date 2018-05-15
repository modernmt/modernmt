package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.cli.utils.FileFormat;
import eu.modernmt.io.IOCorporaUtils;
import eu.modernmt.lang.Language;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;

import java.io.File;

/**
 * Created by davide on 04/07/16.
 */
public class ConvertMain {

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
        public final Language sourceLanguage;
        public final Language targetLanguage;

        private MultilingualCorpus getCorpusInstance(CommandLine cli, String prefix) throws ParseException {
            String formatName = cli.getOptionValue(prefix + "-format");
            FileFormat format = FileFormat.fromName(formatName);

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
            sourceLanguage = cli.hasOption('s') ? Language.fromString(cli.getOptionValue("s")) : null;
            targetLanguage = cli.hasOption('t') ? Language.fromString(cli.getOptionValue("t")) : null;

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
