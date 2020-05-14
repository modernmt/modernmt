package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.io.IOCorporaUtils;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DeduplicationMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().build();
            Option lengthThreshold = Option.builder("l").hasArg().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();
            Option sort = Option.builder().longOpt("sort").hasArgs().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(lengthThreshold);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(outputPath);
            cliOptions.addOption(sort);
        }

        public final Language source;
        public final Language target;
        public final int lengthThreshold;
        public final File[] inputRoots;
        public final File outputRoot;
        public final String[] sortBy;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            source = Language.fromString(cli.getOptionValue('s'));
            target = cli.hasOption('t') ? Language.fromString(cli.getOptionValue('t')) : null;
            lengthThreshold = cli.hasOption("l") ? Integer.parseInt(cli.getOptionValue("l")) : 0;

            String[] roots = cli.getOptionValues("input");
            inputRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                inputRoots[i] = new File(roots[i]);

            outputRoot = new File(cli.getOptionValue("output"));
            sortBy = cli.hasOption("sort") ? cli.getOptionValues("sort") : null;
        }

    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);

        Args args = new Args(_args);

        if (args.target == null) {
            List<Corpus> corpora = Corpora.list(args.source, args.inputRoots);
            if (corpora.isEmpty())
                throw new ParseException("Input path does not contains valid monolingual data");

            if (args.sortBy != null) {
                IOCorporaUtils.countMonolingualLines(corpora);  // pre-compute lines-count value
                corpora.sort(new MonolingualCorporaComparator(args.sortBy));
            }

            ModernMT.training.deduplicateMonolingual(corpora, args.outputRoot, args.lengthThreshold, args.sortBy != null);
        } else {
            LanguageDirection language = new LanguageDirection(args.source, args.target);
            List<MultilingualCorpus> corpora = Corpora.list(language, args.inputRoots);
            if (corpora.isEmpty())
                throw new ParseException("Input path does not contains valid bilingual data");

            if (args.sortBy != null) {
                IOCorporaUtils.countLines(corpora);  // pre-compute lines-count value
                corpora.sort(new MultilingualCorporaComparator(language, args.sortBy));
            }

            ModernMT.training.deduplicate(corpora, args.outputRoot, args.lengthThreshold, args.sortBy != null);
        }
    }

    private static class CorporaComparator {

        private final String[] priorityArray;

        public CorporaComparator(String[] priorityArray) {
            this.priorityArray = priorityArray;
        }

        private int getPriority(String name) {
            for (int i = 0; i < priorityArray.length; i++) {
                if (name.contains(priorityArray[i]))
                    return i;
            }
            return priorityArray.length;
        }

        public int compare(String name1, int lines1, String name2, int lines2) {
            int p1 = getPriority(name1);
            int p2 = getPriority(name2);
            return p1 == p2 ? Integer.compare(lines1, lines2) : Integer.compare(p1, p2);
        }

    }

    private static class MonolingualCorporaComparator extends CorporaComparator implements Comparator<Corpus> {

        public MonolingualCorporaComparator(String[] priorityArray) {
            super(priorityArray);
        }

        @Override
        public int compare(Corpus o1, Corpus o2) {
            return super.compare(o1.getName(), o1.getLineCount(), o2.getName(), o2.getLineCount());
        }
    }

    private static class MultilingualCorporaComparator extends CorporaComparator implements Comparator<MultilingualCorpus> {

        private final LanguageDirection language;

        public MultilingualCorporaComparator(LanguageDirection language, String[] priorityArray) {
            super(priorityArray);
            this.language = language;
        }

        @Override
        public int compare(MultilingualCorpus o1, MultilingualCorpus o2) {
            return super.compare(o1.getName(), o1.getLineCount(language), o2.getName(), o2.getLineCount(language));
        }
    }
}
