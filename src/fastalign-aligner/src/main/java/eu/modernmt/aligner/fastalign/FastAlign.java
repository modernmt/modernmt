package eu.modernmt.aligner.fastalign;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by lucamastrostefano on 15/03/16.
 */
public class FastAlign implements Aligner {

    private static final Logger logger = LogManager.getLogger(FastAlign.class);

    static {
        try {
            System.loadLibrary("fastalign");
            logger.info("Library 'fastalign' loaded successfully");
        } catch (Throwable e) {
            logger.error("Unable to load library 'fastalign'", e);
            throw e;
        }
    }

    private SymmetrizationStrategy strategy = SymmetrizationStrategy.GROW_DIAGONAL_FINAL_AND;
    private long nativeHandle;

    public FastAlign(File model) throws IOException {
        if (!model.isDirectory())
            throw new IOException("Invalid model path: " + model);

        this.nativeHandle = instantiate(model.getAbsolutePath(), Runtime.getRuntime().availableProcessors());
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
    public Alignment getAlignment(Sentence source, Sentence target) throws AlignerException {
        return getAlignment(source, target, strategy);
    }

    @Override
    public Alignment getAlignment(Sentence source, Sentence target, SymmetrizationStrategy strategy) throws AlignerException {
        return parse(align(getIds(source), getIds(target), toInt(strategy)));
    }

    private native int[] align(int[] source, int[] target, int strategy);

    @Override
    public Alignment[] getAlignments(List<Sentence> sources, List<Sentence> targets) throws AlignerException {
        return getAlignments(sources, targets, strategy);
    }

    @Override
    public Alignment[] getAlignments(List<Sentence> sources, List<Sentence> targets, SymmetrizationStrategy strategy) throws AlignerException {
        int[][] sourcesIds = new int[sources.size()][];
        int[][] targetsIds = new int[targets.size()][];

        Iterator<Sentence> sourceIterator = sources.iterator();
        Iterator<Sentence> targetIterator = targets.iterator();

        int i = 0;
        while (sourceIterator.hasNext() && targetIterator.hasNext()) {
            sourcesIds[i] = getIds(sourceIterator.next());
            targetsIds[i] = getIds(targetIterator.next());
            i++;
        }

        int[][] result = new int[sourcesIds.length][];
        align(sourcesIds, targetsIds, result, toInt(strategy));

        Alignment[] alignments = new Alignment[result.length];

        for (int j = 0; j < result.length; j++)
            alignments[j] = parse(result[j]);

        return alignments;
    }

    @Override
    public long getNativeHandle() {
        return nativeHandle;
    }

    private native void align(int[][] sources, int[][] targets, int[][] result, int strategy);

    private static int toInt(SymmetrizationStrategy strategy) {
        switch (strategy) {
            case GROW_DIAGONAL_FINAL_AND:
                return 1;
            case GROW_DIAGONAL:
                return 2;
            case INTERSECT:
                return 3;
            case UNION:
                return 4;
        }

        return 0;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        nativeHandle = dispose(nativeHandle);
    }

    @Override
    public void close() {
        nativeHandle = dispose(nativeHandle);
    }

    private native long dispose(long handle);

    private static int[] getIds(Sentence sentence) {
        Word[] words = sentence.getWords();
        int[] ids = new int[words.length];

        for (int i = 0; i < ids.length; i++) {
            ids[i] = words[i].getId();
        }

        return ids;
    }

    private static Alignment parse(int[] encoded) throws AlignerException {
        if (encoded.length % 2 == 1)
            throw new AlignerException("Invalid native result length: " + encoded.length);

        int size = encoded.length / 2;

        int[] source = new int[size];
        int[] target = new int[size];

        System.arraycopy(encoded, 0, source, 0, size);
        System.arraycopy(encoded, size, target, 0, size);

        return new Alignment(source, target);
    }

}
