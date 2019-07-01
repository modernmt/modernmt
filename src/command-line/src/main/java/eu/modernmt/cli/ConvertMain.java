package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.cli.utils.FileFormat;
import eu.modernmt.io.IOCorporaUtils;
import eu.modernmt.lang.Language2;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MaskedMultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.training.BatchCopyProcess;
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
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option input = Option.builder().longOpt("input").hasArgs().required().build();
            Option inputFormat = Option.builder().longOpt("input-format").hasArg().build();
            Option output = Option.builder().longOpt("output").hasArg().required().build();
            Option outputFormat = Option.builder().longOpt("output-format").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(input);
            cliOptions.addOption(inputFormat);
            cliOptions.addOption(output);
            cliOptions.addOption(outputFormat);
        }

        public final LanguageDirection language;
        public final File input;
        public final File output;
        public final FileFormat inputFormat;
        public final FileFormat outputFormat;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            Language2 source = Language2.fromString(cli.getOptionValue("s"));
            Language2 target = Language2.fromString(cli.getOptionValue("t"));
            language = new LanguageDirection(source, target);

            input = new File(cli.getOptionValue("input"));
            inputFormat = cli.hasOption("input-format") ? FileFormat.fromName(cli.getOptionValue("input-format")) : null;
            output = new File(cli.getOptionValue("output"));
            outputFormat = FileFormat.fromName(cli.getOptionValue("output-format"));
        }
    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);
        Args args = new Args(_args);

        if (args.input.isFile()) {
            MultilingualCorpus input = new MaskedMultilingualCorpus(args.language, args.inputFormat.parse(args.language, args.input));
            MultilingualCorpus output = args.outputFormat.rename(args.language, input, args.output);

            IOCorporaUtils.copy(input, output);
        } else {
            BatchCopyProcess copy = new BatchCopyProcess(new BatchCopyProcess.OutputCorpusFactory() {
                @Override
                public MultilingualCorpus getOutput(MultilingualCorpus corpus) {
                    return args.outputFormat.rename(args.language, corpus, args.output);
                }

                @Override
                public Corpus getOutput(Corpus corpus) {
                    throw new UnsupportedOperationException();
                }
            });

            for (MultilingualCorpus corpus : Corpora.list(args.language, args.input))
                copy.add(new MaskedMultilingualCorpus(args.language, corpus));

            copy.setIoThreads(Math.min(Runtime.getRuntime().availableProcessors(), 32));
            copy.run();
        }
    }
}
