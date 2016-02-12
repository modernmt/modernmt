package eu.modernmt.model;

import eu.modernmt.processing.framework.UnixLineReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    public static List<ParallelFilesCorpus> list(File directory, String sourceLangCode, String targetLangCode) {
        Collection<File> sourceFiles = FileUtils.listFiles(directory, new String[]{sourceLangCode}, false);
        ArrayList<ParallelFilesCorpus> corpora = new ArrayList<>(sourceFiles.size());

        for (File sourceFile : sourceFiles) {
            String name = FilenameUtils.removeExtension(sourceFile.getName());
            corpora.add(new ParallelFilesCorpus(directory, name, sourceLangCode, targetLangCode));
        }

        return corpora;
    }

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParallelFilesCorpus that = (ParallelFilesCorpus) o;

        if (!source.equals(that.source)) return false;
        return target.equals(that.target);

    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + target.hashCode();
        return result;
    }

    @Override
    public ParallelLineReader getContentReader() throws FileNotFoundException {
        return new ParallelFilesLineReader(source, target);
    }

    @Override
    public Reader getContentReader(Locale language) throws IOException {
        File file;

        if (language.equals(sourceLanguage))
            file = source;
        else if (language.equals(targetLanguage))
            file = target;
        else
            throw new IllegalArgumentException("Language " + language.toLanguageTag() + " not found");

        return new InputStreamReader(new FileInputStream(file), "UTF-8");
    }

    @Override
    public ParallelLineWriter getContentWriter(boolean append) throws IOException {
        return new ParallelFilesWriter(append, source, target);
    }

    @Override
    public Writer getContentWriter(Locale language, boolean append) throws IOException {
        File file;

        if (language.equals(sourceLanguage))
            file = source;
        else if (language.equals(targetLanguage))
            file = target;
        else
            throw new IllegalArgumentException("Language " + language.toLanguageTag() + " not found");

        return new FileWriter(file, append);
    }

    private static class ParallelFilesLineReader implements ParallelLineReader {

        private UnixLineReader sourceReader;
        private UnixLineReader targetReader;

        public ParallelFilesLineReader(File source, File target) throws FileNotFoundException {
            boolean success = false;

            try {
                this.sourceReader = new UnixLineReader(new InputStreamReader(new FileInputStream(source), "UTF-8"));
                this.targetReader = new UnixLineReader(new InputStreamReader(new FileInputStream(target), "UTF-8"));

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
            String source = sourceReader.readLine();
            String target = targetReader.readLine();

            if (source == null || target == null)
                return null;

            String[] result = new String[2];
            result[SOURCE_LINE_INDEX] = source;
            result[TARGET_LINE_INDEX] = target;
            return result;
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(this.sourceReader);
            IOUtils.closeQuietly(this.targetReader);
        }
    }

    private static class ParallelFilesWriter implements ParallelLineWriter {

        private FileWriter sourceWriter;
        private FileWriter targetWriter;

        public ParallelFilesWriter(boolean append, File source, File target) throws IOException {
            boolean success = false;

            try {
                this.sourceWriter = new FileWriter(source, append);
                this.targetWriter = new FileWriter(target, append);

                success = true;
            } finally {
                if (!success)
                    this.close();
            }
        }

        @Override
        public void write(String source, String target) throws IOException {
            sourceWriter.write(source);
            sourceWriter.write('\n');

            targetWriter.write(target);
            targetWriter.write('\n');
        }

        @Override
        public void close() throws IOException {
            IOUtils.closeQuietly(this.sourceWriter);
            IOUtils.closeQuietly(this.targetWriter);
        }

    }

}
