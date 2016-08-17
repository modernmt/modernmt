package eu.modernmt.vocabulary;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by davide on 16/08/16.
 */
public class VocabularyBuilder {

    private long nativeHandle;
    private File model;

    public VocabularyBuilder(File model) {
        this.nativeHandle = instantiate();
        this.model = model;
    }

    private native long instantiate();

    public synchronized native int add(String word);

    public synchronized native int[] addLine(String[] line);

    public synchronized List<int[]> addLines(List<String[]> lines) {
        String[][] buffer = new String[lines.size()][];
        lines.toArray(buffer);

        int[][] result = new int[buffer.length][];
        addLines(buffer, result);

        return Arrays.asList(result);
    }

    public synchronized int[][] addLines(String[][] lines) {
        int[][] result = new int[lines.length][];
        addLines(lines, result);

        return result;
    }

    private native void addLines(String[][] lines, int[][] result);

    public synchronized Vocabulary build() throws IOException {
        if (!model.isDirectory())
            FileUtils.forceMkdir(model);

        flush(model.getAbsolutePath());

        return new Vocabulary(model);
    }

    private native void flush(String path);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        nativeHandle = dispose(nativeHandle);
    }

    private native long dispose(long handle);

}
