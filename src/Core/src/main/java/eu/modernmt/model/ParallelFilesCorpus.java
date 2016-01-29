package eu.modernmt.model;

import eu.modernmt.processing.framework.UnixLineReader;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Locale;

/**
 * Created by davide on 28/01/16.
 */
public class ParallelFilesCorpus implements ParallelCorpus {

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private File source;
    private File target;
    private String name;
    private Locale sourceLanguage;
    private Locale targetLanguage;
    private int lineCount = -1;

    public ParallelFilesCorpus(File directory, String name, String sourceLangCode, String targetLangCode) {
        this(directory, name, Locale.forLanguageTag(sourceLangCode), Locale.forLanguageTag(targetLangCode));
    }

    public ParallelFilesCorpus(File directory, String name, Locale sourceLanguage, Locale targetLanguage) {
        this.name = name;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;

        this.source = new File(directory, name + "." + sourceLanguage.toLanguageTag());
        this.target = new File(directory, name + "." + targetLanguage.toLanguageTag());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Locale getSourceLanguage() {
        return sourceLanguage;
    }

    @Override
    public Locale getTargetLanguage() {
        return targetLanguage;
    }

    @Override
    public int getLineCount() throws IOException {
        if (lineCount < 0) {
            synchronized (this) {
                if (lineCount < 0) {
                    FileInputStream stream = null;

                    try {
                        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                        stream = new FileInputStream(this.source);

                        int count = 0;
                        int size;

                        while ((size = stream.read(buffer)) != -1) {
                            for (int i = 0; i < size; i++) {
                                if (buffer[i] == (byte) 0x0A)
                                    count++;
                            }
                        }

                        this.lineCount = count;
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                }
            }
        }

        return this.lineCount;
    }

    @Override
    public ParallelLineReader getContentReader() throws FileNotFoundException {
        return new ParallelFilesLineReader(source, target);
    }


    private static class ParallelFilesLineReader implements ParallelLineReader {

        private UnixLineReader sourceStream;
        private UnixLineReader targetStream;

        public ParallelFilesLineReader(File source, File target) throws FileNotFoundException {
            boolean success = false;

            try {
                this.sourceStream = new UnixLineReader(new InputStreamReader(new FileInputStream(source), "UTF-8"));
                this.targetStream = new UnixLineReader(new InputStreamReader(new FileInputStream(target), "UTF-8"));

                success = true;
            } catch (UnsupportedEncodingException e) {
                throw new Error("Unsupported UTF-8", e);
            } finally {
                if (!success)
                    this.close();
            }
        }

        @Override
        public String[] read() throws IOException {
            String source = sourceStream.readLine();
            String target = targetStream.readLine();

            if (source == null || target == null)
                return null;

            String[] result = new String[2];
            result[SOURCE_LINE_INDEX] = source;
            result[TARGET_LINE_INDEX] = target;
            return result;
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(this.sourceStream);
            IOUtils.closeQuietly(this.targetStream);
        }
    }

}
