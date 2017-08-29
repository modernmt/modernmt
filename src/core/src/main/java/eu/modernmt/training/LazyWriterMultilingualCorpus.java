package eu.modernmt.training;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpusWrapper;

import java.io.IOException;
import java.util.Set;

/**
 * Created by davide on 29/08/17.
 */
public class LazyWriterMultilingualCorpus implements MultilingualCorpusWrapper {

    private final MultilingualCorpus corpus;

    public LazyWriterMultilingualCorpus(MultilingualCorpus corpus) {
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
    public Set<LanguagePair> getLanguages() {
        return corpus.getLanguages();
    }

    @Override
    public int getLineCount(LanguagePair language) {
        return corpus.getLineCount(language);
    }

    @Override
    public MultilingualLineReader getContentReader() throws IOException {
        return corpus.getContentReader();
    }

    @Override
    public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
        return new MultilingualLineWriter() {

            private MultilingualLineWriter writer = null;

            @Override
            public void write(StringPair pair) throws IOException {
                if (writer == null)
                    writer = corpus.getContentWriter(append);
                writer.write(pair);
            }

            @Override
            public void close() throws IOException {
                if (writer != null)
                    writer.close();
            }
        };
    }

    @Override
    public Corpus getCorpus(LanguagePair language, boolean source) {
        return corpus.getCorpus(language, source);
    }
}
