package eu.modernmt.model.corpus.impl.parallel;

import eu.modernmt.io.*;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.corpus.*;
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
    private final LanguageDirection language;

    public ParallelFileCorpus(File directory, String name, LanguageDirection language) {
        this(name, language, new File(directory, name + "." + language.source.toLanguageTag()),
                new File(directory, name + "." + language.target.toLanguageTag()));
    }

    public ParallelFileCorpus(LanguageDirection language, File source, File target) {
        this(FilenameUtils.removeExtension(source.getName()), language, source, target);
    }

    public ParallelFileCorpus(LanguageDirection language, FileProxy source, FileProxy target) {
        this(FilenameUtils.removeExtension(source.getFilename()), language, source, target);
    }

    public ParallelFileCorpus(String name, LanguageDirection language, File source, File target) {
        this(name, language, FileProxy.wrap(source), FileProxy.wrap(target));
    }

    public ParallelFileCorpus(String name, LanguageDirection language, FileProxy source, FileProxy target) {
        this.name = name;
        this.language = language;
        this.source = source;
        this.target = target;
    }

    public LanguageDirection getLanguage() {
        return language;
    }

    public FileProxy getSourceFile() {
        return source;
    }

    public FileProxy getTargetFile() {
        return target;
    }

    @Override
    public Corpus getCorpus(LanguageDirection language, boolean source) {
        if (this.language.equals(language))
            return new FileCorpus(source ? this.source : this.target, name, source ? language.source : language.target);
        else
            throw new UnsupportedLanguageException(language);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TUReader getContentReader() throws IOException {
        return new ParallelFileLineReader(language, source, target);
    }

    @Override
    public TUWriter getContentWriter(boolean append) throws IOException {
        return new ParallelFileTUWriter(append, language, source, target);
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

    private static class ParallelFileLineReader implements TUReader {

        private final LanguageDirection language;
        private final UnixLineReader sourceReader;
        private final UnixLineReader targetReader;
        private int index;

        private ParallelFileLineReader(LanguageDirection language, FileProxy source, FileProxy target) throws IOException {
            this.language = language;

            boolean success = false;

            try {
                this.sourceReader = new UnixLineReader(source.getInputStream(), UTF8Charset.get());
                this.targetReader = new UnixLineReader(target.getInputStream(), UTF8Charset.get());
                this.index = 0;

                success = true;

            } finally {
                if (!success)
                    this.close();
            }
        }

        @Override
        public TranslationUnit read() throws IOException {
            String source = sourceReader.readLine();
            String target = targetReader.readLine();

            if (source == null && target == null) {
                return null;
            } else if (source != null && target != null) {
                this.index++;
                return new TranslationUnit(language, source, target);
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

    private static class ParallelFileTUWriter implements TUWriter {

        private final LanguageDirection language;
        private final LineWriter sourceWriter;
        private final LineWriter targetWriter;

        private ParallelFileTUWriter(boolean append, LanguageDirection language, FileProxy source, FileProxy target) throws IOException {
            this.language = language;

            boolean success = false;

            try {
                this.sourceWriter = new UnixLineWriter(source.getOutputStream(append), UTF8Charset.get());
                this.targetWriter = new UnixLineWriter(target.getOutputStream(append), UTF8Charset.get());

                success = true;
            } finally {
                if (!success)
                    this.close();
            }
        }

        @Override
        public void write(TranslationUnit tu) throws IOException {
            if (language.isEqualOrMoreGenericThan(tu.language)) {
                sourceWriter.writeLine(tu.source);
                targetWriter.writeLine(tu.target);
            } else if (language.isEqualOrMoreGenericThan(tu.language.reversed())) {
                sourceWriter.writeLine(tu.target);
                targetWriter.writeLine(tu.source);
            } else {
                throw new IOException("Unsupported language: " + tu.language);
            }
        }

        @Override
        public void flush() throws IOException {
            sourceWriter.flush();
            targetWriter.flush();
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(this.sourceWriter);
            IOUtils.closeQuietly(this.targetWriter);
        }
    }
}
