package eu.modernmt.model.corpus;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by davide on 31/07/17.
 */
public abstract class BaseMultilingualCorpus implements MultilingualCorpus {

    private Map<LanguagePair, Integer> _counts = null;

    private Map<LanguagePair, Integer> getCounts() {
        if (_counts == null) {
            synchronized (this) {
                if (_counts == null)
                    try {
                        _counts = Corpora.countLines(this);
                    } catch (IOException e) {
                        _counts = new HashMap<>();
                    }
            }
        }

        return _counts;
    }

    @Override
    public Set<LanguagePair> getLanguages() {
        return getCounts().keySet();
    }

    @Override
    public int getLineCount(LanguagePair language) {
        Integer count = getCounts().get(language);
        return count == null ? 0 : count;
    }

    @Override
    public Corpus getCorpus(LanguagePair language, boolean source) {
        return new CorpusView(language, source);
    }

    protected final class CorpusView implements Corpus {

        private final LanguagePair direction;
        private final boolean source;

        public CorpusView(LanguagePair direction, boolean source) {
            this.direction = direction;
            this.source = source;
        }

        @Override
        public String getName() {
            return BaseMultilingualCorpus.this.getName();
        }

        @Override
        public Language getLanguage() {
            return source ? direction.source : direction.target;
        }

        @Override
        public LineReader getContentReader() throws IOException {
            return new LineReader() {

                private final MultilingualLineReader reader = BaseMultilingualCorpus.this.getContentReader();

                @Override
                public String readLine() throws IOException {
                    StringPair pair;
                    do {
                        pair = reader.read();
                    } while (pair != null && !pair.language.equals(direction));

                    if (pair == null)
                        return null;
                    else
                        return source ? pair.source : pair.target;
                }

                @Override
                public void close() throws IOException {
                    reader.close();
                }

            };
        }

        @Override
        public LineWriter getContentWriter(boolean append) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Reader getRawContentReader() throws IOException, UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }

}
