package eu.modernmt.aligner.fastalign;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
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
            System.loadLibrary("mmt_fastalign");
        } catch (Throwable e) {
            logger.error("Unable to load library 'mmt_fastalign'", e);
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
        int[] alignment = align(XUtils.toTokensArray(source), XUtils.toTokensArray(target), XUtils.toInt(strategy));
        return XUtils.parseAlignment(alignment);
    }

    @Override
    public Alignment[] getAlignments(List<Sentence> sources, List<Sentence> targets) throws AlignerException {
        return getAlignments(sources, targets, strategy);
    }

    @Override
    public Alignment[] getAlignments(List<Sentence> sources, List<Sentence> targets, SymmetrizationStrategy strategy) throws AlignerException {
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

        align(sourceArray, targetArray, result, XUtils.toInt(strategy));

        for (int j = 0; j < result.length; j++)
            alignments[j] = XUtils.parseAlignment(result[j]);

        return alignments;
    }

    private native int[] align(String[] source, String[] target, int strategy);

    private native void align(String[][] sources, String[][] targets, int[][] result, int strategy);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        nativeHandle = dispose(nativeHandle);
    }

    @Override
    public void close() {
        // Nothing to do
    }

    private native long dispose(long handle);

}
