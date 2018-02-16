package eu.modernmt.training;

import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
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
    private final LanguageIndex languages;

    public MultilingualCorpusMask(LanguagePair language, MultilingualCorpus corpus) {
        this.corpus = corpus;
        this.languages = new LanguageIndex(language);
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
                    pair.language = languages.mapToBestMatching(pair.language);

                    if (pair.language == null)
                        continue;

                    if (languages.contains(pair.language))
                        return pair;

                    if (languages.contains(pair.language.reversed()))
                        return reverse(pair);
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
        pair.language = pair.language.reversed();

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
                LanguagePair mapped = languages.mapToBestMatching(pair.language);

                if (languages.contains(mapped))
                    writer.write(new StringPair(mapped, pair.source, pair.target, pair.timestamp));
                else if (languages.contains(mapped.reversed()))
                    writer.write(new StringPair(mapped.reversed(), pair.target, pair.source, pair.timestamp));
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

}
