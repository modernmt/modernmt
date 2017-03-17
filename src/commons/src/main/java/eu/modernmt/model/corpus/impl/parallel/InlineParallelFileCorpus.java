package eu.modernmt.model.corpus.impl.parallel;

import eu.modernmt.io.FileProxy;
import eu.modernmt.io.UnixLineReader;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Created by davide on 22/12/16.
 */
public class InlineParallelFileCorpus implements BilingualCorpus {

    private final String name;
    private final Locale sourceLanguage;
    private final Locale targetLanguage;
    private final FileProxy file;

    public InlineParallelFileCorpus(Locale sourceLanguage, Locale targetLanguage, FileProxy file) {
        this(FilenameUtils.removeExtension(file.getFilename()), sourceLanguage, targetLanguage, file);
    }

    public InlineParallelFileCorpus(String name, Locale sourceLanguage, Locale targetLanguage, FileProxy file) {
        this.name = name;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.file = file;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public BilingualLineReader getContentReader() throws IOException {
        return new InlinePlainTextBilingualLineReader(file.getInputStream());
    }

    @Override
    public BilingualLineWriter getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Corpus getSourceCorpus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Corpus getTargetCorpus() {
        throw new UnsupportedOperationException();
    }

    private static class InlinePlainTextBilingualLineReader implements BilingualLineReader {

        private final UnixLineReader reader;

        public InlinePlainTextBilingualLineReader(InputStream inputStream) {
            this.reader = new UnixLineReader(inputStream, Charset.defaultCharset());
        }

        @Override
        public StringPair read() throws IOException {
            String source = reader.readLine();
            if (source == null)
                return null;
            String target = reader.readLine();
            if (target == null)
                throw new IOException("Invalid inline plain-text bilingual file: missing target sentence");

            return new StringPair(source, target);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }

}
