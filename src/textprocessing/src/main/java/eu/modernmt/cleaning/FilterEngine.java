package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 15/02/18.
 */
public class FilterEngine {

    public static class Builder {
        private ArrayList<Filter> filters = new ArrayList<>();
        private ArrayList<Normalizer> normalizers = new ArrayList<>();

        public void add(Filter filter) {
            this.filters.add(filter);
        }

        public void add(Normalizer normalizer) {
            this.normalizers.add(normalizer);
        }

        public FilterEngine build() {
            Filter[] filters = this.filters.toArray(new Filter[this.filters.size()]);
            Normalizer[] normalizers = this.normalizers.toArray(new Normalizer[this.normalizers.size()]);
            return new FilterEngine(filters, normalizers);
        }
    }

    private final Filter[] filters;
    private final Normalizer[] normalizers;

    private FilterEngine(Filter[] filters, Normalizer[] normalizers) {
        this.filters = filters;
        this.normalizers = normalizers;
    }

    List<Filter.Initializer> getInitializers() {
        ArrayList<Filter.Initializer> initializers = new ArrayList<>(filters.length);

        for (Filter filter : filters) {
            Filter.Initializer initializer = filter.getInitializer();
            if (initializer != null)
                initializers.add(initializer);
        }

        return initializers;
    }

    public void normalize(MultilingualCorpus.StringPair pair, int index) {
        for (Normalizer normalizer : normalizers)
            normalizer.normalize(pair, index);
    }

    public boolean accept(MultilingualCorpus.StringPair pair, int index) throws IOException {
        for (Filter filter : filters) {
            if (!filter.accept(pair, index)) {
                return false;
            }
        }

        return true;
    }

    public void clear() {
        for (Filter filter : filters)
            filter.clear();
    }

}
