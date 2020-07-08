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
    public TUReader getContentReader() throws IOException {
        return new TUReader() {

            private final TUReader reader = corpus.getContentReader();

            @Override
            public TranslationUnit read() throws IOException {
                TranslationUnit tu;
                while ((tu = reader.read()) != null) {
                    if (language.isEqualOrMoreGenericThan(tu.language)) {
                        tu.language = language;
                        return tu;
                    } else if (language.isEqualOrMoreGenericThan(tu.language.reversed())) {
                        tu = reverse(tu);
                        tu.language = language;
                        return tu;
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

    private static TranslationUnit reverse(TranslationUnit tu) {
        String temp = tu.target;
        tu.target = tu.source;
        tu.source = temp;

        return tu;
    }

    @Override
    public TUWriter getContentWriter(boolean append) throws IOException {
        return new TUWriter() {

            private final TUWriter writer = corpus.getContentWriter(append);

            @Override
            public void write(TranslationUnit tu) throws IOException {
                if (language.isEqualOrMoreGenericThan(tu.language))
                    writer.write(new TranslationUnit(tu.tuid, language, tu.source, tu.target, tu.timestamp));
                else if (language.isEqualOrMoreGenericThan(tu.language.reversed()))
                    writer.write(new TranslationUnit(tu.tuid, language, tu.target, tu.source, tu.timestamp));
                else
                    throw new UnsupportedLanguageException(tu.language);
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
