package eu.modernmt.model.corpus.impl.parallel;

import eu.modernmt.io.*;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.impl.BaseMultilingualCorpus;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 24/02/16.
 */
public class ParallelFileCorpus extends BaseMultilingualCorpus {

    private final FileProxy source;
    private final FileProxy target;
    private final String name;
    private final LanguagePair language;

    public ParallelFileCorpus(File directory, String name, LanguagePair language) {
        this(name, language, new File(directory, name + "." + language.source.toLanguageTag()),
                new File(directory, name + "." + language.target.toLanguageTag()));
    }

    public ParallelFileCorpus(LanguagePair language, File source, File target) {
        this(FilenameUtils.removeExtension(source.getName()), language, source, target);
    }

    public ParallelFileCorpus(LanguagePair language, FileProxy source, FileProxy target) {
        this(FilenameUtils.removeExtension(source.getFilename()), language, source, target);
    }

    public ParallelFileCorpus(String name, LanguagePair language, File source, File target) {
        this(name, language, FileProxy.wrap(source), FileProxy.wrap(target));
    }

    public ParallelFileCorpus(String name, LanguagePair language, FileProxy source, FileProxy target) {
        this.name = name;
        this.language = language;
        this.source = source;
        this.target = target;
    }

    public LanguagePair getLanguage() {
        return language;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MultilingualLineReader getContentReader() throws IOException {
        return new ParallelFileLineReader(language, source, target);
    }

    @Override
    public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
        return new ParallelFileLineWriter(append, source, target);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParallelFileCorpus that = (ParallelFileCorpus) o;

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
    public String toString() {
        return name + '[' + language.toString() + ']';
    }

    private static class ParallelFileLineReader implements MultilingualLineReader {

        private final LanguagePair language;
        private UnixLineReader sourceReader;
        private UnixLineReader targetReader;
        private int index;

        private ParallelFileLineReader(LanguagePair language, FileProxy source, FileProxy target) throws IOException {
            this.language = language;

            boolean success = false;

            try {
                this.sourceReader = new UnixLineReader(source.getInputStream(), DefaultCharset.get());
                this.targetReader = new UnixLineReader(target.getInputStream(), DefaultCharset.get());
                this.index = 0;

                success = true;

            } finally {
                if (!success)
                    this.close();
            }
        }

        @Override
        public StringPair read() throws IOException {
            String source = sourceReader.readLine();
            String target = targetReader.readLine();

            if (source == null && target == null) {
                return null;
            } else if (source != null && target != null) {
                this.index++;
                return new StringPair(language, source, target);
            } else {
                throw new IOException("Invalid parallel corpus: unmatched line at " + (this.index + 1));
            }
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(this.sourceReader);
            IOUtils.closeQuietly(this.targetReader);
        }
    }

    private static class ParallelFileLineWriter implements MultilingualLineWriter {

        private LineWriter sourceWriter;
        private LineWriter targetWriter;

        private ParallelFileLineWriter(boolean append, FileProxy source, FileProxy target) throws IOException {
            boolean success = false;

            try {
                this.sourceWriter = new UnixLineWriter(source.getOutputStream(append), DefaultCharset.get());
                this.targetWriter = new UnixLineWriter(target.getOutputStream(append), DefaultCharset.get());

                success = true;
            } finally {
                if (!success)
                    this.close();
            }
        }

        @Override
        public void write(StringPair pair) throws IOException {
            sourceWriter.writeLine(pair.source);
            targetWriter.writeLine(pair.target);
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(this.sourceWriter);
            IOUtils.closeQuietly(this.targetWriter);
        }

    }
}
