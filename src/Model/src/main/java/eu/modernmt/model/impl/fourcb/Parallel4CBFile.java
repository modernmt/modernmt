package eu.modernmt.model.impl.fourcb;

import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.model.Corpus;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by davide on 14/03/16.
 */
public class Parallel4CBFile implements BilingualCorpus {

    private final File source;
    private final File target;
    private final String name;
    private final Locale sourceLanguage;
    private final Locale targetLanguage;
    private int lineCount = -1;

    public Parallel4CBFile(Locale sourceLanguage, File source, Locale targetLanguage, File target) {
        this(FilenameUtils.removeExtension(source.getName()), sourceLanguage, source, targetLanguage, target);
    }

    public Parallel4CBFile(String name, Locale sourceLanguage, File source, Locale targetLanguage, File target) {
        this.name = name;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.source = source;
        this.target = target;
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
                    this.lineCount = BilingualCorpus.getLineCount(this);
                }
            }
        }

        return this.lineCount;
    }

    @Override
    public BilingualLineReader getContentReader() throws IOException {
        return new Parallel4CBFileLineReader(source, target);
    }

    @Override
    public BilingualLineWriter getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Corpus getSourceCorpus() {
        // TODO: not implemented yet
        throw new UnsupportedOperationException();
    }

    @Override
    public Corpus getTargetCorpus() {
        // TODO: not implemented yet
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return name + ".4cb{" + sourceLanguage.toLanguageTag() + '|' + targetLanguage.toLanguageTag() + '}';
    }

    private static class Parallel4CBFileLineReader implements BilingualLineReader {

        private FourCBFileReader sourceReader;
        private FourCBFileReader targetReader;

        private Parallel4CBFileLineReader(File source, File target) throws IOException {
            boolean success = false;

            try {
                this.sourceReader = new FourCBFileReader(source);
                this.targetReader = new FourCBFileReader(target);

                success = true;
            } finally {
                if (!success)
                    this.close();
            }
        }

        @Override
        public StringPair read() throws IOException {
            FourCBFileReader.Line4CB source = sourceReader.readLineWithMetadata();
            FourCBFileReader.Line4CB target = targetReader.readLineWithMetadata();

            if (source == null || target == null)
                return null;

            if (!equals(source.id, target.id))
                throw new IOException("Mismatching ids in 4CB file: " + source.id + " and " + target.id);

            return new StringPair(source.line, target.line);
        }

        private static final boolean equals(String id1, String id2) {
            if (id1 == null) return id2 == null;
            if (id2 == null) return false;

            return id1.equals(id2);
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(this.sourceReader);
            IOUtils.closeQuietly(this.targetReader);
        }
    }

}
