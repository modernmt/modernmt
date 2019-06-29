package eu.modernmt.model.corpus;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.lang.UnsupportedLanguageException;

import java.io.IOException;

public class MaskedMultilingualCorpus extends BaseMultilingualCorpus implements MultilingualCorpusWrapper {

    private final MultilingualCorpus corpus;
    private final LanguageDirection language;

    public MaskedMultilingualCorpus(LanguageDirection language, MultilingualCorpus corpus) {
        this.corpus = corpus;
        this.language = language;
    }

    @Override
    public String getName() {
        return corpus.getName();
    }

    @Override
    public MultilingualCorpus.MultilingualLineReader getContentReader() throws IOException {
        return new MultilingualCorpus.MultilingualLineReader() {

            private final MultilingualCorpus.MultilingualLineReader reader = corpus.getContentReader();

            @Override
            public MultilingualCorpus.StringPair read() throws IOException {
                MultilingualCorpus.StringPair pair;
                while ((pair = reader.read()) != null) {
                    if (language.isEqualOrMoreGenericThan(pair.language)) {
                        pair.language = language;
                        return pair;
                    } else if (language.isEqualOrMoreGenericThan(pair.language.reversed())) {
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

    private static MultilingualCorpus.StringPair reverse(MultilingualCorpus.StringPair pair) {
        String temp = pair.target;
        pair.target = pair.source;
        pair.source = temp;

        return pair;
    }

    @Override
    public MultilingualCorpus.MultilingualLineWriter getContentWriter(boolean append) throws IOException {
        return new MultilingualCorpus.MultilingualLineWriter() {

            private final MultilingualCorpus.MultilingualLineWriter writer = corpus.getContentWriter(append);

            @Override
            public void write(MultilingualCorpus.StringPair pair) throws IOException {
                if (language.isEqualOrMoreGenericThan(pair.language))
                    writer.write(new MultilingualCorpus.StringPair(language, pair.source, pair.target, pair.timestamp));
                else if (language.isEqualOrMoreGenericThan(pair.language.reversed()))
                    writer.write(new MultilingualCorpus.StringPair(language, pair.target, pair.source, pair.timestamp));
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
