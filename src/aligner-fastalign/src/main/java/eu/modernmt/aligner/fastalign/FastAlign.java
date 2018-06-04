package eu.modernmt.aligner.fastalign;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by lucamastrostefano on 15/03/16.
 */
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

    private static LanguagePair getLanguagePairFromFilename(File file) throws IOException {
        String encoded = FilenameUtils.removeExtension(file.getName());
        String[] parts = encoded.split("__");
        if (parts.length != 2)
            throw new IOException("Invalid FastAlign model: " + file);

        return new LanguagePair(Language.fromString(parts[0]), Language.fromString(parts[1]));
    }

    public FastAlign(File modelPath) throws IOException {
        if (!modelPath.isDirectory())
            throw new IOException("Invalid model path: " + modelPath);

        File[] paths = modelPath.listFiles(path -> path.isDirectory() && path.getName().endsWith(".mdl"));

        if (paths == null || paths.length == 0)
            throw new IOException("Could not load any FastAlign model from path " + modelPath);

        int threads = Runtime.getRuntime().availableProcessors();

        this.models = new HashMap<>(paths.length);

        for (File path : paths) {
            long nativeHandle = instantiate(path.getAbsolutePath(), threads);
            LanguagePair pair = getLanguagePairFromFilename(path);

            if (pair.source.getRegion() != null || pair.target.getRegion() != null)
                throw new IOException("Cannot specify region for model language tag in model: " + path);

            this.models.put(LanguageKey.parse(pair), nativeHandle);
        }
    }

    private native long instantiate(String modelDirectory, int threads);

    @Override
    public boolean isSupported(LanguagePair direction) {
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
    public Alignment getAlignment(LanguagePair language, Sentence source, Sentence target) throws AlignerException {
        return getAlignment(language, source, target, strategy);
    }

    @Override
    public Alignment getAlignment(LanguagePair language, Sentence source, Sentence target, SymmetrizationStrategy strategy) throws AlignerException {
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
    public Alignment[] getAlignments(LanguagePair language, List<? extends Sentence> sources, List<? extends Sentence> targets) throws AlignerException {
        return getAlignments(language, sources, targets, strategy);
    }

    @Override
    public Alignment[] getAlignments(LanguagePair language, List<? extends Sentence> sources, List<? extends Sentence> targets, SymmetrizationStrategy strategy) throws AlignerException {
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

        public static LanguageKey parse(LanguagePair pair) {
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
}
