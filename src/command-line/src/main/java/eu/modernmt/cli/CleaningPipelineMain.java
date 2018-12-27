package eu.modernmt.cli;

import eu.modernmt.cleaning.CorporaCleaning;
import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.cli.utils.FileFormat;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.training.BatchCopyProcess;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.List;

/**
 * Created by davide on 17/12/15.
 */
public class CleaningPipelineMain {

    public enum Filter {
        NORMALIZE, PUNCTUATION, ODD_SENTENCES, DRAFTS, SENTENCE_LENGTH, VERBATIM, NUMERIC, LANGUAGE
    }

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();
            Option outputFormat = Option.builder().longOpt("output-format").hasArgs().build();
            Option filters = Option.builder().longOpt("filters").hasArgs().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(outputPath);
            cliOptions.addOption(outputFormat);
            cliOptions.addOption(filters);
        }

        public final Language source;
        public final Language target;
        public final File[] inputRoots;
        public final File outputRoot;
        public final FileFormat outputFormat;
        public final Filter[] filters;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            source = Language.fromString(cli.getOptionValue('s'));
            target = cli.hasOption('t') ? Language.fromString(cli.getOptionValue('t')) : null;

            String[] roots = cli.getOptionValues("input");
            inputRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                inputRoots[i] = new File(roots[i]);

            outputRoot = new File(cli.getOptionValue("output"));
            outputFormat = cli.hasOption("output-format") ? FileFormat.fromName(cli.getOptionValue("output-format")) : null;

            if (cli.hasOption("filters")) {
                String[] values = cli.getOptionValues("filters");
                filters = new Filter[values.length];

                for (int i = 0; i < filters.length; i++)
                    filters[i] = Filter.valueOf(values[i].toUpperCase());
            } else {
                filters = null;
            }
        }

    }

    private static CorporaCleaning.Options getOptions(Filter[] filters) {
        CorporaCleaning.Options options;

        if (filters == null) {
            options = CorporaCleaning.Options.defaultOptionsForTraining();
        } else {
            options = new CorporaCleaning.Options();

            for (Filter filter : filters) {
                switch (filter) {
                    case NORMALIZE:
                        options.normalize = true;
                        break;
                    case PUNCTUATION:
                        options.filterByPunctuation = true;
                        break;
                    case ODD_SENTENCES:
                        options.filterOddSentences = true;
                        break;
                    case DRAFTS:
                        options.filterDrafts = true;
                        break;
                    case SENTENCE_LENGTH:
                        options.filterBySentenceLength = true;
                        break;
                    case NUMERIC:
                        options.filterNumericSentences = true;
                        break;
                    case VERBATIM:
                        options.filterVerbatimTranslations = true;
                        break;
                    case LANGUAGE:
                        options.filterByLanguage = true;
                        break;
                }
            }
        }

        return options;
    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);
        CorporaCleaning.Options options = getOptions(args.filters);

        FileUtils.deleteDirectory(args.outputRoot);
        FileUtils.forceMkdir(args.outputRoot);

        if (args.target == null) {
            List<Corpus> corpora = Corpora.list(args.source, args.inputRoots);
            if (corpora.isEmpty())
                throw new ParseException("Input path does not contains valid monolingual data");

            ModernMT.training.cleanMonolingual(corpora, args.outputRoot, options);
        } else {
            LanguagePair language = new LanguagePair(args.source, args.target);
            List<MultilingualCorpus> corpora = Corpora.list(language, args.inputRoots);
            if (corpora.isEmpty())
                throw new ParseException("Input path does not contains valid bilingual data");

            if (args.outputFormat == null) {
                ModernMT.training.clean(corpora, args.outputRoot, options);
            } else {
                ModernMT.training.clean(corpora, options, new BatchCopyProcess.OutputCorpusFactory() {

                    @Override
                    public MultilingualCorpus getOutput(MultilingualCorpus corpus) {
                        return args.outputFormat.rename(args.source, args.target, corpus, args.outputRoot);
                    }

                    @Override
                    public Corpus getOutput(Corpus corpus) {
                        throw new UnsupportedOperationException();
                    }

                });
            }
        }
    }

}
