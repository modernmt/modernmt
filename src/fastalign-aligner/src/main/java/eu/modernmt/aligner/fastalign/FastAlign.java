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

    private long nativeHandle;

    public FastAlign(File model) throws IOException {
        if (!model.isFile())
            throw new IOException("Invalid model path: " + model);

        this.nativeHandle = instantiate(model.getAbsolutePath(), Runtime.getRuntime().availableProcessors());
    }

    private native long instantiate(String modelFile, int threads);

    @Override
    public Alignment getAlignment(Sentence source, Sentence target) throws AlignerException {
        return parse(align(getIds(source), getIds(target)));
    }

    private native int[] align(int[] source, int[] target);

    @Override
    public Alignment[] getAlignments(List<Sentence> sources, List<Sentence> targets) throws AlignerException {
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
        align(sourcesIds, targetsIds, result);

        Alignment[] alignments = new Alignment[result.length];

        for (int j = 0; j < result.length; j++)
            alignments[j] = parse(result[j]);

        return alignments;
    }

    private native void align(int[][] sources, int[][] targets, int[][] result);

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
