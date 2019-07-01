package eu.modernmt.aligner.fastalign;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class FastAlign implements Aligner {

    private static final Logger logger = LogManager.getLogger(FastAlign.class);

    static {
        try {
            System.loadLibrary("mmt_fastalign");
        } catch (Throwable e) {
            logger.error("Unable to load library 'mmt_fastalign'", e);
            throw e;
        }
    }

    private SymmetrizationStrategy strategy = SymmetrizationStrategy.GROW_DIAGONAL_FINAL_AND;
    private final HashMap<LanguageKey, Long> models;

    private static Collection<LanguageDirection> parseLanguagesFromFilename(File file) throws IOException {
        String encoded = FilenameUtils.removeExtension(file.getName());
        String[] parts = encoded.split("__");
        if (parts.length != 2)
            throw new IOException("Invalid FastAlign model: " + file);

        String[] sources = parts[0].split("_");
        String[] targets = parts[1].split("_");
        HashSet<LanguageDirection> languages = new HashSet<>();

        for (String source : sources) {
            for (String target : targets) {
                languages.add(new LanguageDirection(Language.fromString(source), Language.fromString(target)));
            }
        }

        return languages;
    }

    private Map<File, Long> load(File[] paths) {
        int nproc = Runtime.getRuntime().availableProcessors();
        int threads = Math.min(paths.length, nproc);
        int alignerThreads = Math.max(1, (int) (nproc * 3. / 4.));

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            Future<?>[] futures = new Future[paths.length];
            for (int i = 0; i < futures.length; i++)
                futures[i] = executor.submit(new InitTask(paths[i], alignerThreads));

            HashMap<File, Long> models = new HashMap<>(paths.length);
            for (int i = 0; i < futures.length; i++) {
                try {
                    models.put(paths[i], (Long) futures[i].get());
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted execution", e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException)
                        throw (RuntimeException) cause;
                    else
                        throw new Error("Unexpected exception", cause);
                }
            }

            return models;
        } finally {
            executor.shutdownNow();
        }
    }

    public FastAlign(File modelPath) throws IOException {
        if (!modelPath.isDirectory())
            throw new IOException("Invalid model path: " + modelPath);

        File[] paths = modelPath.listFiles(path -> path.isFile() && path.getName().endsWith(".mdl"));

        if (paths == null || paths.length == 0)
            throw new IOException("Could not load any FastAlign model from path " + modelPath);

        logger.info("Loading FastAlign models");
        long now = System.currentTimeMillis();
        Map<File, Long> handlers = load(paths);
        logger.info("Loaded " + handlers.size() + " FastAlign models in " + (int) ((System.currentTimeMillis() - now) / 1000) + "s");

        this.models = new HashMap<>(paths.length);
        for (Map.Entry<File, Long> entry : handlers.entrySet()) {
            File path = entry.getKey();
            Long nativeHandle = entry.getValue();

            for (LanguageDirection pair : parseLanguagesFromFilename(path)) {
                if (!pair.source.isLanguageOnly() || !pair.target.isLanguageOnly())
                    throw new IOException("FastAlign models support language-only tags, found '" + pair + "' for path: " + path);

                this.models.put(LanguageKey.parse(pair), nativeHandle);
            }
        }
    }

    private native long instantiate(String modelFile, int threads);

    @Override
    public boolean isSupported(LanguageDirection direction) {
        LanguageKey key = LanguageKey.parse(direction);
        return models.containsKey(key) || models.containsKey(key.reversed());
    }

    @Override
    public void setDefaultSymmetrizationStrategy(SymmetrizationStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public SymmetrizationStrategy getDefaultSymmetrizationStrategy() {
        return strategy;
    }

    @Override
    public Alignment getAlignment(LanguageDirection language, Sentence source, Sentence target) throws AlignerException {
        return getAlignment(language, source, target, strategy);
    }

    @Override
    public Alignment getAlignment(LanguageDirection language, Sentence source, Sentence target, SymmetrizationStrategy strategy) throws AlignerException {
        boolean reversed = false;

        LanguageKey key = LanguageKey.parse(language);
        Long nativeHandle = models.get(key);

        if (nativeHandle == null) {
            reversed = true;
            nativeHandle = models.get(key.reversed());
        }

        int[][] output = new int[1][];
        float score = align(nativeHandle, reversed, XUtils.toTokensArray(source), XUtils.toTokensArray(target), XUtils.toInt(strategy), output);
        return XUtils.parseAlignment(output[0], score);
    }

    private native float align(long nativeHandle, boolean reversed, String[] source, String[] target, int strategy, int[][] result);

    @Override
    public Alignment[] getAlignments(LanguageDirection language, List<? extends Sentence> sources, List<? extends Sentence> targets) throws AlignerException {
        return getAlignments(language, sources, targets, strategy);
    }

    @Override
    public Alignment[] getAlignments(LanguageDirection language, List<? extends Sentence> sources, List<? extends Sentence> targets, SymmetrizationStrategy strategy) throws AlignerException {
        boolean reversed = false;

        LanguageKey key = LanguageKey.parse(language);
        Long nativeHandle = models.get(key);

        if (nativeHandle == null) {
            reversed = true;
            nativeHandle = models.get(key.reversed());
        }

        String[][] sourceArray = new String[sources.size()][];
        String[][] targetArray = new String[targets.size()][];

        Iterator<? extends Sentence> sourceIterator = sources.iterator();
        Iterator<? extends Sentence> targetIterator = targets.iterator();

        int i = 0;
        while (sourceIterator.hasNext() && targetIterator.hasNext()) {
            sourceArray[i] = XUtils.toTokensArray(sourceIterator.next());
            targetArray[i] = XUtils.toTokensArray(targetIterator.next());
            i++;
        }

        int[][] result = new int[sourceArray.length][];
        Alignment[] alignments = new Alignment[result.length];

        float[] scores = align(nativeHandle, reversed, sourceArray, targetArray, XUtils.toInt(strategy), result);

        for (int j = 0; j < result.length; j++)
            alignments[j] = XUtils.parseAlignment(result[j], scores[j]);

        return alignments;
    }

    private native float[] align(long nativeHandle, boolean reversed, String[][] sources, String[][] targets, int strategy, int[][] outputAlignment);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        for (long nativeHandle : models.values())
            dispose(nativeHandle);

        models.clear();
    }

    @Override
    public void close() {
        // Nothing to do
    }

    private native long dispose(long handle);

    private static final class LanguageKey {

        public static LanguageKey parse(LanguageDirection pair) {
            return new LanguageKey(pair.source.getLanguage(), pair.target.getLanguage());
        }

        private final String source;
        private final String target;

        public LanguageKey(String source, String target) {
            this.source = source;
            this.target = target;
        }

        public LanguageKey reversed() {
            return new LanguageKey(target, source);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LanguageKey that = (LanguageKey) o;

            if (!source.equals(that.source)) return false;
            return target.equals(that.target);
        }

        @Override
        public int hashCode() {
            int result = source.hashCode();
            result = 31 * result + target.hashCode();
            return result;
        }
    }

    private final class InitTask implements Callable<Long> {

        private final File path;
        private final int threads;

        private InitTask(File path, int threads) {
            this.path = path;
            this.threads = threads;
        }

        @Override
        public Long call() {
            return instantiate(path.getAbsolutePath(), threads);
        }

    }
}
