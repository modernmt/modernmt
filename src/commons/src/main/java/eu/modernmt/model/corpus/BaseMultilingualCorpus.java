package eu.modernmt.model.corpus;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by davide on 31/07/17.
 */
public abstract class BaseMultilingualCorpus implements MultilingualCorpus {

    private Map<LanguageDirection, Integer> _counts = null;

    private Map<LanguageDirection, Integer> getCounts() {
        if (_counts == null) {
            synchronized (this) {
                if (_counts == null)
                    try {
                        _counts = count();
                    } catch (IOException e) {
                        _counts = new HashMap<>();
                    }
            }
        }

        return _counts;
    }

    private Map<LanguageDirection, Integer> count() throws IOException {
        Map<LanguageDirection, Counter> counts = new HashMap<>();

        TUReader reader = null;

        try {
            reader = getContentReader();

            TranslationUnit tu;
            while ((tu = reader.read()) != null) {
                counts.computeIfAbsent(tu.language, k -> new Counter()).count++;
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }

        Map<LanguageDirection, Integer> result = new HashMap<>(counts.size());
        for (Map.Entry<LanguageDirection, Counter> entry : counts.entrySet())
            result.put(entry.getKey(), entry.getValue().count);

        return result;
    }

    private static final class Counter {
        public int count = 0;
    }

    @Override
    public Set<LanguageDirection> getLanguages() {
        return getCounts().keySet();
    }

    @Override
    public int getLineCount(LanguageDirection language) {
        Integer count = getCounts().get(language);
        return count == null ? 0 : count;
    }

    @Override
    public Corpus getCorpus(LanguageDirection language, boolean source) {
        return new CorpusView(language, source);
    }

    protected final class CorpusView implements Corpus {

        private final LanguageDirection direction;
        private final boolean source;

        public CorpusView(LanguageDirection direction, boolean source) {
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
        public int getLineCount() {
            return BaseMultilingualCorpus.this.getLineCount(direction);
        }

        @Override
        public LineReader getContentReader() throws IOException {
            return new LineReader() {

                private final TUReader reader = BaseMultilingualCorpus.this.getContentReader();

                @Override
                public String readLine() throws IOException {
                    TranslationUnit tu;
                    do {
                        tu = reader.read();
                    } while (tu != null && !tu.language.equals(direction));

                    if (tu == null)
                        return null;
                    else
                        return source ? tu.source : tu.target;
                }

                @Override
                public void close() throws IOException {
                    reader.close();
                }

            };
        }

        @Override
        public LineWriter getContentWriter(boolean append) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Reader getRawContentReader() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }

}
