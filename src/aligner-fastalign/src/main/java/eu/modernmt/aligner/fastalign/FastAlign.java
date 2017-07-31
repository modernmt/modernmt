package eu.modernmt.aligner.fastalign;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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
    private final HashMap<LanguagePair, Long> models;

    private static LanguagePair getLanguagePairFromFilename(File file) throws IOException {
        String[] parts = file.getName().split("\\.");
        if (parts.length != 3)
            throw new IOException("Invalid FastAlign model: " + file);

        return new LanguagePair(Locale.forLanguageTag(parts[0]), Locale.forLanguageTag(parts[1]));
    }

    public FastAlign(File modelPath) throws IOException {
        if (!modelPath.isDirectory())
            throw new IOException("Invalid model path: " + modelPath);

        File[] paths = modelPath.listFiles(path -> path.isDirectory() && path.getName().endsWith(".fmdl"));

        if (paths == null || paths.length == 0)
            throw new IOException("Could not load any FastAlign model from path " + modelPath);

        int threads = Runtime.getRuntime().availableProcessors();

        this.models = new HashMap<>(paths.length);
        for (File path : paths) {
            long nativeHandle = instantiate(path.getAbsolutePath(), threads);
            LanguagePair pair = getLanguagePairFromFilename(path);

            this.models.put(pair, nativeHandle);
        }
    }

    private native long instantiate(String modelDirectory, int threads);

    @Override
    public void setDefaultSymmetrizationStrategy(SymmetrizationStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public SymmetrizationStrategy getDefaultSymmetrizationStrategy() {
        return strategy;
    }

    @Override
    public Alignment getAlignment(LanguagePair direction, Sentence source, Sentence target) throws AlignerException {
        return getAlignment(direction, source, target, strategy);
    }

    @Override
    public Alignment getAlignment(LanguagePair direction, Sentence source, Sentence target, SymmetrizationStrategy strategy) throws AlignerException {
        boolean reversed = false;
        Long nativeHandle = models.get(direction);

        if (nativeHandle == null) {
            reversed = true;
            nativeHandle = models.get(direction.reversed());
        }

        if (nativeHandle == null)
            throw new UnsupportedLanguageException(direction);

        int[] alignment = align(nativeHandle, reversed, XUtils.toTokensArray(source), XUtils.toTokensArray(target), XUtils.toInt(strategy));
        return XUtils.parseAlignment(alignment);
    }

    private native int[] align(long nativeHandle, boolean reversed, String[] source, String[] target, int strategy);

    @Override
    public Alignment[] getAlignments(LanguagePair direction, List<Sentence> sources, List<Sentence> targets) throws AlignerException {
        return getAlignments(direction, sources, targets, strategy);
    }

    @Override
    public Alignment[] getAlignments(LanguagePair direction, List<Sentence> sources, List<Sentence> targets, SymmetrizationStrategy strategy) throws AlignerException {
        boolean reversed = false;
        Long nativeHandle = models.get(direction);

        if (nativeHandle == null) {
            reversed = true;
            nativeHandle = models.get(direction.reversed());
        }

        if (nativeHandle == null)
            throw new UnsupportedLanguageException(direction);

        String[][] sourceArray = new String[sources.size()][];
        String[][] targetArray = new String[targets.size()][];

        Iterator<Sentence> sourceIterator = sources.iterator();
        Iterator<Sentence> targetIterator = targets.iterator();

        int i = 0;
        while (sourceIterator.hasNext() && targetIterator.hasNext()) {
            sourceArray[i] = XUtils.toTokensArray(sourceIterator.next());
            targetArray[i] = XUtils.toTokensArray(targetIterator.next());
            i++;
        }

        int[][] result = new int[sourceArray.length][];
        Alignment[] alignments = new Alignment[result.length];

        align(nativeHandle, reversed, sourceArray, targetArray, result, XUtils.toInt(strategy));

        for (int j = 0; j < result.length; j++)
            alignments[j] = XUtils.parseAlignment(result[j]);

        return alignments;
    }

    private native void align(long nativeHandle, boolean reversed, String[][] sources, String[][] targets, int[][] result, int strategy);

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

}
