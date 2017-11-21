package eu.modernmt.cleaning.filters.ngrams;

import eu.modernmt.cleaning.MultilingualCorpusFilter;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 17/11/16.
 */
public class RareNgramFilter implements MultilingualCorpusFilter {

    private final HashMap<LanguagePair, Vocabulary> ngrams = new HashMap<>();

    @Override
    public FilterInitializer getInitializer() {
        return new FilterInitializer() {

            private final HashMap<LanguagePair, Vocabulary.Builder> vocabs = new HashMap<>();

            @Override
            public void onBegin() {
                clear();
            }

            @Override
            public void onPair(MultilingualCorpus corpus, MultilingualCorpus.StringPair pair, int index) throws IOException {
                Vocabulary.Builder builder = vocabs.computeIfAbsent(pair.language, key -> new Vocabulary.Builder());
                builder.add(pair);
            }

            @Override
            public void onEnd() {
                for (Map.Entry<LanguagePair, Vocabulary.Builder> entry : vocabs.entrySet()) {
                    Vocabulary vocabulary = entry.getValue().build(0.9);
                    ngrams.put(entry.getKey(), vocabulary);
                }
            }

        };
    }

    @Override
    public boolean accept(MultilingualCorpus.StringPair pair, int index) throws IOException {
        Vocabulary vocabulary = ngrams.get(pair.language);
        return vocabulary.accept(pair, .3);
    }

    @Override
    public void clear() {
        ngrams.clear();
    }

}
