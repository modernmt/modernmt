package eu.modernmt.cli;

import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.io.Corpora;
import eu.modernmt.io.FileProxy;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.*;
import eu.modernmt.model.corpus.impl.parallel.CompactFileCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import eu.modernmt.training.bloomfilter.CorporaBloomFilter;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MergeDatasetsMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option inputPath = Option.builder().longOpt("input").hasArgs().required().build();
            Option outputPath = Option.builder().longOpt("output").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(inputPath);
            cliOptions.addOption(outputPath);
        }

        public final LanguageDirection language;
        public final File[] inputRoots;
        public final File outputRoot;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            Language source = Language.fromString(cli.getOptionValue('s'));
            Language target = Language.fromString(cli.getOptionValue('t'));
            language = new LanguageDirection(source, target);

            String[] roots = cli.getOptionValues("input");
            inputRoots = new File[roots.length];
            for (int i = 0; i < roots.length; i++)
                inputRoots[i] = new File(roots[i]);
            if (roots.length != 2)
                throw new ParseException("\"input\" argument accept exactly 2 arguments");

            outputRoot = new File(cli.getOptionValue("output"));
        }

    }

    private static Map<String, MultilingualCorpus> load(LanguageDirection language, File root) throws IOException {
        List<MultilingualCorpus> corpora = Corpora.list(language, root);
        HashMap<String, MultilingualCorpus> output = new HashMap<>(corpora.size());
        for (MultilingualCorpus corpus : corpora) {
            String key = corpus.getName();
            if (output.containsKey(key))
                throw new IllegalArgumentException("Duplicate corpus: " + key);
            corpus = new MaskedMultilingualCorpus(language, corpus);
            output.put(key, corpus);
        }

        return output;
    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);
        Args args = new Args(_args);

        FileUtils.deleteDirectory(args.outputRoot);
        FileUtils.forceMkdir(args.outputRoot);

        Map<String, MultilingualCorpus> in1 = load(args.language, args.inputRoots[0]);
        Map<String, MultilingualCorpus> in2 = load(args.language, args.inputRoots[1]);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        try {
            List<Future<Void>> futures = new ArrayList<>();

            // in1
            for (Map.Entry<String, MultilingualCorpus> entry : in1.entrySet()) {
                String key = entry.getKey();
                MultilingualCorpus c1 = entry.getValue();
                MultilingualCorpus c2 = in2.get(key);

                if (c2 == null) {
                    futures.add(executor.submit(() -> copy(c1, args.outputRoot)));
                } else {
                    futures.add(executor.submit(() -> merge(args.language, c1, c2, args.outputRoot)));
                    in2.remove(key);
                }
            }

            // in2
            for (MultilingualCorpus c2 : in2.values()) {
                futures.add(executor.submit(() -> copy(c2, args.outputRoot)));
            }

            // wait for completion
            for (Future<Void> future : futures)
                future.get();
        } finally {
            executor.shutdownNow();
        }
    }

    private static Void copy(MultilingualCorpus corpus, File output) throws IOException {
        corpus = MultilingualCorpusWrapper.unwrap(corpus);
        if (corpus instanceof TMXCorpus) {
            File file = ((FileProxy.NativeFileProxy) ((TMXCorpus) corpus).getFile()).getFile();
            File dest = new File(output, file.getName());
            FileUtils.moveFile(file, dest);
        } else if (corpus instanceof CompactFileCorpus) {
            File file = ((FileProxy.NativeFileProxy) ((CompactFileCorpus) corpus).getFile()).getFile();
            File dest = new File(output, file.getName());
            FileUtils.moveFile(file, dest);
        } else {
            File source = ((FileProxy.NativeFileProxy) ((ParallelFileCorpus) corpus).getSourceFile()).getFile();
            File target = ((FileProxy.NativeFileProxy) ((ParallelFileCorpus) corpus).getTargetFile()).getFile();
            File sourceDest = new File(output, source.getName());
            File targetDest = new File(output, target.getName());
            FileUtils.moveFile(source, sourceDest);
            FileUtils.moveFile(target, targetDest);
        }

        return null;
    }

    private static Void merge(LanguageDirection language, MultilingualCorpus c1, MultilingualCorpus c2, File output) throws IOException {
        long lines = c1.getLineCount(language) + c2.getLineCount(language);
        CorporaBloomFilter bloomFilter = new CorporaBloomFilter(lines);

        MultilingualCorpus out = Corpora.rename(c1, output);
        try (TUWriter writer = out.getContentWriter(false)) {
            try (TUReader reader = c1.getContentReader()) {
                TranslationUnit tu;
                while ((tu = reader.read()) != null) {
                    bloomFilter.put(tu);
                    writer.write(tu);
                }
            }

            try (TUReader reader = c2.getContentReader()) {
                TranslationUnit tu;
                while ((tu = reader.read()) != null) {
                    if (!bloomFilter.contains(tu))
                        writer.write(tu);
                }
            }
        }

        delete(c1);
        delete(c2);

        return null;
    }

    private static void delete(MultilingualCorpus corpus) {
        corpus = MultilingualCorpusWrapper.unwrap(corpus);

        if (corpus instanceof TMXCorpus) {
            File file = ((FileProxy.NativeFileProxy) ((TMXCorpus) corpus).getFile()).getFile();
            FileUtils.deleteQuietly(file);
        } else if (corpus instanceof CompactFileCorpus) {
            File file = ((FileProxy.NativeFileProxy) ((CompactFileCorpus) corpus).getFile()).getFile();
            FileUtils.deleteQuietly(file);
        } else {
            File source = ((FileProxy.NativeFileProxy) ((ParallelFileCorpus) corpus).getSourceFile()).getFile();
            File target = ((FileProxy.NativeFileProxy) ((ParallelFileCorpus) corpus).getTargetFile()).getFile();
            FileUtils.deleteQuietly(source);
            FileUtils.deleteQuietly(target);
        }
    }

}
