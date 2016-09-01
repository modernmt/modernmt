package eu.modernmt.vocabulary;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by davide on 16/08/16.
 */
public class Vocabulary implements IVocabulary {

    public static final int VOCABULARY_UNKNOWN_WORD = 0;
    public static final int VOCABULARY_WORD_ID_START = 1000;

    private long nativeHandle;

    public Vocabulary(File model) throws IOException {
        if (!model.isDirectory())
            FileUtils.forceMkdir(model);

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
