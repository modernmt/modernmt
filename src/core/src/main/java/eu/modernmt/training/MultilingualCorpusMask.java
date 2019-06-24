package eu.modernmt.training;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpusWrapper;

import java.io.IOException;

/**
 * Created by davide on 18/08/17.
 */
public class MultilingualCorpusMask extends BaseMultilingualCorpus implements MultilingualCorpusWrapper {

    private final MultilingualCorpus corpus;
    private final LanguageDirection language;

    public MultilingualCorpusMask(LanguageDirection language, MultilingualCorpus corpus) {
        this.corpus = corpus;
        this.language = language;
    }

    @Override
    public String getName() {
        return corpus.getName();
    }

    @Override
    public MultilingualLineReader getContentReader() throws IOException {
        return new MultilingualLineReader() {

            private final MultilingualLineReader reader = corpus.getContentReader();

            @Override
            public StringPair read() throws IOException {
                StringPair pair;
                while ((pair = reader.read()) != null) {
                    if (match(pair.language)) {
                        pair.language = language;
                        return pair;
                    } else if (match(pair.language.reversed())) {
                        pair = reverse(pair);
                        pair.language = language;
                        return pair;
                    }
                }

                return null;
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        };
    }

    private static StringPair reverse(StringPair pair) {
        String temp = pair.target;
        pair.target = pair.source;
        pair.source = temp;

        return pair;
    }

    @Override
    public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
        return new MultilingualLineWriter() {

            private final MultilingualLineWriter writer = corpus.getContentWriter(append);

            @Override
            public void write(StringPair pair) throws IOException {
                if (match(pair.language))
                    writer.write(new StringPair(language, pair.source, pair.target, pair.timestamp));
                else if (match(pair.language.reversed()))
                    writer.write(new StringPair(language, pair.target, pair.source, pair.timestamp));
                else
                    throw new UnsupportedLanguageException(pair.language);
            }

            @Override
            public void flush() throws IOException {
                writer.flush();
            }

            @Override
            public void close() throws IOException {
                writer.close();
            }
        };
    }

    @Override
    public MultilingualCorpus getWrappedCorpus() {
        return corpus;
    }

    private boolean match(LanguageDirection pair) {
        return match(pair.source, this.language.source) && match(pair.target, this.language.target);
    }

    private static boolean match(Language test, Language ref) {
        return ref.getLanguage().equals(test.getLanguage()) &&
                (ref.getRegion() == null || ref.getRegion().equals(test.getRegion()));
    }

}
