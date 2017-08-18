package eu.modernmt.model.corpus;

import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.lang.UnsupportedLanguageException;

import java.io.IOException;

/**
 * Created by davide on 18/08/17.
 */
public class MultilingualCorpusMask extends BaseMultilingualCorpus {

    private final MultilingualCorpus corpus;
    private final LanguageIndex languages;

    public MultilingualCorpusMask(LanguageIndex languages, MultilingualCorpus corpus) {
        this.corpus = corpus;
        this.languages = languages;
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
                    pair.language = languages.map(pair.language);

                    if (pair.language != null)
                        return pair;
                }

                return null;
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        };
    }

    @Override
    public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
        return new MultilingualLineWriter() {

            private final MultilingualLineWriter writer = corpus.getContentWriter(append);

            @Override
            public void write(StringPair pair) throws IOException {
                LanguagePair mapped = languages.map(pair.language);
                if (mapped == null)
                    throw new UnsupportedLanguageException(pair.language);

                writer.write(new StringPair(mapped, pair.source, pair.target, pair.timestamp));
            }

            @Override
            public void close() throws IOException {
                writer.close();
            }
        };
    }

}
