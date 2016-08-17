package eu.modernmt.vocabulary;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by davide on 16/08/16.
 */
public class Vocabulary {

    private long nativeHandle;

    public Vocabulary(File model) throws IOException {
        if (!model.isDirectory())
            FileUtils.forceMkdir(model);

        this.nativeHandle = instantiate(model.getAbsolutePath());
    }

    private native long instantiate(String modelPath);

    public native int getId(String word, boolean putIfAbsent);

    public native int[] encodeLine(String[] line, boolean putIfAbsent);

    public List<int[]> encodeLines(List<String[]> lines, boolean putIfAbsent) {
        String[][] buffer = new String[lines.size()][];
        lines.toArray(buffer);

        int[][] result = new int[buffer.length][];
        encodeLines(buffer, result, putIfAbsent);
        return Arrays.asList(result);
    }

    public int[][] encodeLines(String[][] lines, boolean putIfAbsent) {
        int[][] result = new int[lines.length][];
        encodeLines(lines, result, putIfAbsent);

        return result;
    }

    private native void encodeLines(String[][] lines, int[][] output, boolean putIfAbsent);

    public native String getWord(int id);

    public native String[] decodeLine(int[] line);

    public List<String[]> decodeLines(List<int[]> lines) {
        int[][] buffer = new int[lines.size()][];
        lines.toArray(buffer);

        String[][] result = new String[buffer.length][];
        decodeLines(buffer, result);

        return Arrays.asList(result);
    }

    public String[][] decodeLines(int[][] lines) {
        String[][] result = new String[lines.length][];
        decodeLines(lines, result);

        return result;
    }

    private native void decodeLines(int[][] lines, String[][] result);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        nativeHandle = dispose(nativeHandle);
    }

    protected native long dispose(long handle);

}
