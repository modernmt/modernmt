package eu.modernmt.vocabulary.rocksdb;

import eu.modernmt.vocabulary.Vocabulary;
import eu.modernmt.vocabulary.VocabularyBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by davide on 01/09/16.
 */
public class RocksDBVocabulary implements Vocabulary {

    private static final Logger logger = LogManager.getLogger(RocksDBVocabulary.class);

    static {
        try {
            System.loadLibrary("mmt_vocabulary");
        } catch (Throwable e) {
            logger.error("Unable to load library 'mmt_vocabulary'", e);
            throw e;
        }
    }

    public static VocabularyBuilder newBuilder(File modelPath) {
        return new RocksDBVocabularyBuilder(modelPath);
    }

    public static VocabularyBuilder newBuilder(File modelPath, int initialCapacity) {
        return new RocksDBVocabularyBuilder(modelPath, initialCapacity);
    }

    private long nativeHandle;

    public RocksDBVocabulary(File model) throws IOException {
        if (!model.exists())
            FileUtils.touch(model);
        this.nativeHandle = instantiate(model.getAbsolutePath());
    }

    private native long instantiate(String modelPath);

    @Override
    public native int lookup(String word, boolean putIfAbsent);

    @Override
    public native int[] lookupLine(String[] line, boolean putIfAbsent);

    @Override
    public List<int[]> lookupLines(List<String[]> lines, boolean putIfAbsent) {
        String[][] buffer = new String[lines.size()][];
        lines.toArray(buffer);

        int[][] result = new int[buffer.length][];
        lookupLines(buffer, result, putIfAbsent);
        return Arrays.asList(result);
    }

    @Override
    public int[][] lookupLines(String[][] lines, boolean putIfAbsent) {
        int[][] result = new int[lines.length][];
        lookupLines(lines, result, putIfAbsent);

        return result;
    }

    private native void lookupLines(String[][] lines, int[][] output, boolean putIfAbsent);

    @Override
    public native String reverseLookup(int id);

    @Override
    public native String[] reverseLookupLine(int[] line);

    @Override
    public List<String[]> reverseLookupLines(List<int[]> lines) {
        int[][] buffer = new int[lines.size()][];
        lines.toArray(buffer);

        String[][] result = new String[buffer.length][];
        reverseLookupLines(buffer, result);

        return Arrays.asList(result);
    }

    @Override
    public String[][] reverseLookupLines(int[][] lines) {
        String[][] result = new String[lines.length][];
        reverseLookupLines(lines, result);

        return result;
    }

    @Override
    public long getNativeHandle() {
        return nativeHandle;
    }

    private native void reverseLookupLines(int[][] lines, String[][] result);

    @Override
    public void close() throws IOException {
        nativeHandle = dispose(nativeHandle);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        nativeHandle = dispose(nativeHandle);
    }

    protected native long dispose(long handle);

}
