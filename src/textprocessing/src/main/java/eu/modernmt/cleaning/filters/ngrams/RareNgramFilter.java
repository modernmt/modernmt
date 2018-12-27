package eu.modernmt.cleaning.filters.ngrams;

import eu.modernmt.cleaning.CorpusFilter;
import eu.modernmt.lang.Language;

/**
 * Created by davide on 17/11/16.
 */
public class RareNgramFilter implements CorpusFilter {

    private Vocabulary vocab = null;

    @Override
    public Initializer getInitializer(Language language) {
        return new Initializer() {

            private final Vocabulary.Builder builder = new Vocabulary.Builder();

            @Override
            public void onBegin() {
                vocab = null;
            }

            @Override
            public void onLine(String line, int index) {
                builder.add(line);
            }

            @Override
            public void onEnd() {
                vocab = builder.build(.9);
            }

        };
    }

    @Override
    public boolean accept(String line, int index) {
        return vocab.accept(line, .3);
    }

    @Override
    public void clear() {
        vocab = null;
    }

}
