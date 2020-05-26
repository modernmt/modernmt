package eu.modernmt.io;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.CorpusWrapper;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpusWrapper;
import eu.modernmt.model.corpus.impl.parallel.CompactFileCorpus;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by davide on 04/05/17.
 */
public class Corpora {

    public static final String TMX_EXTENSION = "tmx";
    public static final String COMPACT_EXTENSION = "cfc";

    private static final int MAX_IO_THREADS = 10;

    // Stats -----------------------------------------------------------------------------------------------------------

    public static FileStats stats(MultilingualCorpus corpus) {
        return FileStats.of(MultilingualCorpusWrapper.unwrap(corpus));
    }

    public static FileStats stats(Corpus corpus) {
        return FileStats.of(CorpusWrapper.unwrap(corpus));
    }

    // List ------------------------------------------------------------------------------------------------------------

    public static List<Corpus> list(Language language, File... roots) throws IOException {
        String tag = language.toLanguageTag();

        ArrayList<Corpus> corpora = new ArrayList<>();

        for (File folder : roots) {
            if (!folder.isDirectory())
                throw new IOException(folder + " is not a valid folder");

            File[] files = folder.listFiles();
            if (files == null) break;

            for (File file : files) {
                FileStats stats = FileStats.of(file);
                if (!stats.extension.equalsIgnoreCase(tag) || stats.name.isEmpty())
                    continue;

                FileCorpus corpus = new FileCorpus(new FileProxy.NativeFileProxy(file, stats.gzipped), stats.name, language);
                corpora.add(corpus);
            }
        }

        return corpora;
    }

    public static List<MultilingualCorpus> list(LanguageDirection language, File... roots) throws IOException {
        ArrayList<MultilingualCorpus> output = new ArrayList<>();

        HashMap<File, ParallelFileCorpusBuilder> builders = new HashMap<>();

        for (File directory : roots) {
            File[] files = directory.listFiles();
            if (files == null) continue;

            for (File file : files) {
                FileStats stats = FileStats.of(file);
                if (stats.name.isEmpty() || stats.extension.isEmpty())
                    continue;

                if (TMX_EXTENSION.equalsIgnoreCase(stats.extension)) {
                    output.add(new TMXCorpus(stats.name, new FileProxy.NativeFileProxy(file, stats.gzipped)));
                } else if (COMPACT_EXTENSION.equalsIgnoreCase(stats.extension)) {
                    output.add(new CompactFileCorpus(stats.name, new FileProxy.NativeFileProxy(file, stats.gzipped)));
                } else {
                    Language extLanguage;
                    try {
                        extLanguage = Language.fromString(stats.extension);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    File key = new File(directory, stats.name);
                    FileProxy fileProxy = new FileProxy.NativeFileProxy(file, stats.gzipped);
                    ParallelFileCorpusBuilder builder = builders.computeIfAbsent(key, ParallelFileCorpusBuilder::new);

                    if (language.source.isEqualOrMoreGenericThan(extLanguage))
                        builder.setSourceFile(fileProxy);
                    else if (language.target.isEqualOrMoreGenericThan(extLanguage))
                        builder.setTargetFile(fileProxy);
                }
            }
        }

        for (ParallelFileCorpusBuilder builder : builders.values()) {
            ParallelFileCorpus corpus = builder.getParallelFileCorpus(language);
            if (corpus != null)
                output.add(corpus);
        }

        return output;
    }

    // Rename ----------------------------------------------------------------------------------------------------------

    public static MultilingualCorpus rename(MultilingualCorpus corpus, File folder) {
        return rename(corpus, folder, corpus.getName());
    }

    public static MultilingualCorpus rename(MultilingualCorpus corpus, File folder, String name) {
        corpus = MultilingualCorpusWrapper.unwrap(corpus);
        FileStats stats = FileStats.of(corpus);

        switch (stats.type) {
            case TMX:
                return new TMXCorpus(nativeFile(folder, name, TMX_EXTENSION, stats.gzipped));
            case PARALLEL:
                LanguageDirection language = ((ParallelFileCorpus) corpus).getLanguage();
                return new ParallelFileCorpus(name, language,
                        nativeFile(folder, name, language.source.toLanguageTag(), stats.gzipped),
                        nativeFile(folder, name, language.target.toLanguageTag(), stats.gzipped));
            case COMPACT:
                return new CompactFileCorpus(nativeFile(folder, name, COMPACT_EXTENSION, stats.gzipped));
            default:
                throw new Error("unknown type");
        }
    }

    public static Corpus rename(Corpus corpus, File folder) {
        return rename(corpus, folder, corpus.getName());
    }

    public static Corpus rename(Corpus corpus, File folder, String name) {
        Language language = corpus.getLanguage();
        FileStats stats = FileStats.of(CorpusWrapper.unwrap(corpus));
        return new FileCorpus(nativeFile(folder, name, language.toLanguageTag(), stats.gzipped), name, language);
    }

    // Line count ------------------------------------------------------------------------------------------------------

    public static Map<Language, Long> countMonolingualLines(Collection<Corpus> corpora) throws IOException {
        return countMonolingualLines(corpora, Runtime.getRuntime().availableProcessors());
    }

    public static Map<Language, Long> countMonolingualLines(Collection<Corpus> corpora, int threads) throws IOException {
        ArrayList<Callable<Map<Language, Long>>> tasks = new ArrayList<>();
        for (Corpus corpus : corpora) {
            tasks.add(() -> {
                Language language = corpus.getLanguage();
                return Collections.singletonMap(language, (long) corpus.getLineCount());
            });
        }

        return count(tasks, threads);
    }

    public static Map<LanguageDirection, Long> countLines(Collection<MultilingualCorpus> corpora) throws IOException {
        return countLines(corpora, Runtime.getRuntime().availableProcessors());
    }

    public static Map<LanguageDirection, Long> countLines(Collection<MultilingualCorpus> corpora, int threads) throws IOException {
        ArrayList<Callable<Map<LanguageDirection, Long>>> tasks = new ArrayList<>();
        for (MultilingualCorpus corpus : corpora) {
            tasks.add(() -> {
                Set<LanguageDirection> languages = corpus.getLanguages();
                HashMap<LanguageDirection, Long> counts = new HashMap<>(languages.size());

                for (LanguageDirection language : languages)
                    counts.put(language, (long) corpus.getLineCount(language));

                return counts;
            });
        }

        return count(tasks, threads);
    }

    private static <T> Map<T, Long> count(Collection<Callable<Map<T, Long>>> tasks, int threads) throws IOException {
        ExecutorService executor = null;

        try {
            executor = threads > 1 ? Executors.newFixedThreadPool(threads) : Executors.newSingleThreadExecutor();

            ArrayList<Future<Map<T, Long>>> futures = new ArrayList<>(tasks.size());

            for (Callable<Map<T, Long>> task : tasks) {
                futures.add(executor.submit(task));
            }

            Map<T, Long> result = new HashMap<>();

            for (Future<Map<T, Long>> future : futures) {
                try {
                    for (Map.Entry<T, Long> count : future.get().entrySet()) {
                        Long old = result.get(count.getKey());
                        result.put(count.getKey(), (old == null ? 0L : old) + count.getValue());
                    }
                } catch (ExecutionException e) {
                    unwrapException(e);
                } catch (InterruptedException e) {
                    throw new IOException("Interrupted queue", e);
                }
            }

            return result;
        } finally {
            if (executor != null) {
                executor.shutdown();

                try {
                    if (!executor.awaitTermination(1, TimeUnit.SECONDS))
                        executor.shutdownNow();
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                }
            }
        }
    }

    // Word count ------------------------------------------------------------------------------------------------------

    private static class Counter {

        public long value = 0;

    }

    public static Map<LanguageDirection, Long> wordCount(MultilingualCorpus corpus) throws IOException {
        Map<LanguageDirection, Counter> counts = doWordCount(corpus);
        return getCounts(counts);
    }

    public static Map<LanguageDirection, Long> wordCount(Collection<? extends MultilingualCorpus> corpora) throws IOException {
        return wordCount(corpora, Runtime.getRuntime().availableProcessors());
    }

    public static Map<LanguageDirection, Long> wordCount(Collection<? extends MultilingualCorpus> corpora, int threads) throws IOException {
        threads = Math.min(threads, MAX_IO_THREADS);

        ExecutorService executor = threads > 1 ? Executors.newFixedThreadPool(threads) : Executors.newSingleThreadExecutor();
        ExecutorCompletionService<Map<LanguageDirection, Counter>> results = new ExecutorCompletionService<>(executor);

        for (MultilingualCorpus corpus : corpora)
            results.submit(() -> doWordCount(corpus));

        Map<LanguageDirection, Counter> accumulator = null;

        try {
            for (int i = 0; i < corpora.size(); i++) {
                try {
                    Map<LanguageDirection, Counter> result = results.take().get();

                    if (accumulator == null) {
                        accumulator = result;
                    } else {
                        for (Map.Entry<LanguageDirection, Counter> e : result.entrySet())
                            accumulator.computeIfAbsent(e.getKey(), key -> new Counter()).value += e.getValue().value;
                    }
                } catch (ExecutionException e) {
                    unwrapException(e);
                } catch (InterruptedException e) {
                    throw new IOException("Execution interrupted", e);
                }
            }
        } finally {
            executor.shutdownNow();
        }

        return accumulator == null ? Collections.emptyMap() : getCounts(accumulator);
    }

    private static Map<LanguageDirection, Counter> doWordCount(MultilingualCorpus corpus) throws IOException {
        HashMap<LanguageDirection, Counter> counts = new HashMap<>();

        MultilingualCorpus.MultilingualLineReader reader = null;

        try {
            reader = corpus.getContentReader();

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                Counter counter = counts.computeIfAbsent(pair.language, key -> new Counter());
                counter.value += WordCounter.count(pair.source);
            }

            return counts;
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private static Map<LanguageDirection, Long> getCounts(Map<LanguageDirection, Counter> counts) {
        HashMap<LanguageDirection, Long> result = new HashMap<>(counts.size());
        for (Map.Entry<LanguageDirection, Counter> entry : counts.entrySet())
            result.put(entry.getKey(), entry.getValue().value);
        return result;
    }

    // Copy ------------------------------------------------------------------------------------------------------------

    public static void copy(Corpus source, Corpus destination, long linesLimit, boolean append) throws IOException {
        LineReader reader = null;
        LineWriter writer = null;

        try {
            reader = source.getContentReader();
            writer = destination.getContentWriter(append);
            String line;
            long lines = 0;
            while ((line = reader.readLine()) != null && lines++ < linesLimit)
                writer.writeLine(line);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

    public static void copy(Corpus source, Corpus destination, long linesLimit) throws IOException {
        copy(source, destination, linesLimit, false);
    }

    public static void copy(Corpus source, Corpus destination, boolean append) throws IOException {
        copy(source, destination, Long.MAX_VALUE, append);
    }

    public static void copy(Corpus source, Corpus destination) throws IOException {
        copy(source, destination, Long.MAX_VALUE);
    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination, long pairsLimit, boolean append) throws IOException {
        MultilingualCorpus.MultilingualLineReader reader = null;
        MultilingualCorpus.MultilingualLineWriter writer = null;

        try {
            reader = source.getContentReader();
            writer = destination.getContentWriter(append);

            MultilingualCorpus.StringPair pair;
            long pairs = 0;
            while ((pair = reader.read()) != null && pairs++ < pairsLimit)
                writer.write(pair);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination, long linesLimit) throws IOException {
        copy(source, destination, linesLimit, false);
    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination, boolean append) throws IOException {
        copy(source, destination, Long.MAX_VALUE, append);
    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination) throws IOException {
        copy(source, destination, Long.MAX_VALUE);
    }

    // Utils -----------------------------------------------------------------------------------------------------------

    private static void unwrapException(ExecutionException e) throws IOException {
        Throwable cause = e.getCause();

        if (cause instanceof IOException) {
            throw (IOException) cause;
        } else if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        } else {
            throw new Error("Unexpected exception", cause);
        }
    }

    private static class ParallelFileCorpusBuilder {

        public final String name;
        private FileProxy sourceFile = null;
        private FileProxy targetFile = null;

        public ParallelFileCorpusBuilder(File base) {
            this.name = base.getName();
        }

        public void setSourceFile(FileProxy sourceFile) throws IOException {
            if (this.sourceFile != null)
                throw new IOException("Duplicated entry file: " + sourceFile);
            this.sourceFile = sourceFile;
        }

        public void setTargetFile(FileProxy targetFile) throws IOException {
            if (this.targetFile != null)
                throw new IOException("Duplicated entry file: " + targetFile);
            this.targetFile = targetFile;
        }

        public ParallelFileCorpus getParallelFileCorpus(LanguageDirection language) {
            if (sourceFile == null || targetFile == null)
                return null;
            return new ParallelFileCorpus(name, language, sourceFile, targetFile);
        }
    }

    private static FileProxy.NativeFileProxy nativeFile(File folder, String name, String ext, boolean gzipped) {
        String filename = name + '.' + ext;
        if (gzipped) filename += ".gz";
        return new FileProxy.NativeFileProxy(new File(folder, filename), gzipped);
    }

}
