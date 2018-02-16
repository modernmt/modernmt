package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;

/**
 * Created by davide on 15/02/18.
 */
public class StringPairFilter {

    private final FilterEngine filter;

    public StringPairFilter(FilterEngine filter) {
        this.filter = filter;

        if (!this.filter.getInitializers().isEmpty())
            throw new IllegalArgumentException("Invalid filters for StringPairFilter");
    }

    public void normalize(MultilingualCorpus.StringPair pair) {
        this.filter.normalize(pair, 0);
    }

    public boolean accept(MultilingualCorpus.StringPair pair) throws IOException {
        return this.filter.accept(pair, 0);
    }


}
