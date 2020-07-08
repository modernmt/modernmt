package eu.modernmt.model.corpus;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.*;

import java.io.IOException;
import java.util.Set;

/**
 * Created by davide on 29/08/17.
 */
public class LazyMultilingualCorpus implements MultilingualCorpusWrapper {

    private final MultilingualCorpus corpus;

    public LazyMultilingualCorpus(MultilingualCorpus corpus) {
        this.corpus = corpus;
    }

    @Override
    public MultilingualCorpus getWrappedCorpus() {
        return corpus;
    }

    @Override
    public String getName() {
        return corpus.getName();
    }

    @Override
    public Set<LanguageDirection> getLanguages() {
        return corpus.getLanguages();
    }

    @Override
    public int getLineCount(LanguageDirection language) {
        return corpus.getLineCount(language);
    }

    @Override
    public TUReader getContentReader() throws IOException {
        return corpus.getContentReader();
    }

    @Override
    public TUWriter getContentWriter(boolean append) throws IOException {
        return new TUWriter() {

            private TUWriter writer = null;

            @Override
            public void write(TranslationUnit tu) throws IOException {
                if (writer == null)
                    writer = corpus.getContentWriter(append);
                writer.write(tu);
            }

            @Override
            public void flush() throws IOException {
                if (writer != null)
                    writer.flush();
            }

            @Override
            public void close() throws IOException {
                if (writer != null)
                    writer.close();
            }
        };
    }

    @Override
    public Corpus getCorpus(LanguageDirection language, boolean source) {
        return corpus.getCorpus(language, source);
    }
}
