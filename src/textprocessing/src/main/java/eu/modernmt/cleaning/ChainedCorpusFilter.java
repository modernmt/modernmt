package eu.modernmt.cleaning;

import eu.modernmt.lang.Language2;

import java.util.ArrayList;

public class ChainedCorpusFilter implements CorpusFilter, CorpusNormalizer {

    public static class Builder {
        private ArrayList<CorpusFilter> filters = new ArrayList<>();
        private ArrayList<CorpusNormalizer> normalizers = new ArrayList<>();

        public void add(CorpusFilter filter) {
            this.filters.add(filter);
        }

        public void add(CorpusNormalizer normalizer) {
            this.normalizers.add(normalizer);
        }

        public ChainedCorpusFilter build() {
            CorpusFilter[] filters = this.filters.toArray(new CorpusFilter[0]);
            CorpusNormalizer[] normalizers = this.normalizers.toArray(new CorpusNormalizer[0]);
            return new ChainedCorpusFilter(normalizers, filters);
        }
    }

    private final CorpusNormalizer[] normalizers;
    private final CorpusFilter[] filters;

    private ChainedCorpusFilter(CorpusNormalizer[] normalizers, CorpusFilter[] filters) {
        this.normalizers = normalizers;
        this.filters = filters;
    }

    // - CorpusNormalizer ----------------------------------------------------------------------------------------------

    @Override
    public String normalize(String line) {
        for (CorpusNormalizer normalizer : normalizers)
            line = normalizer.normalize(line);
        return line;
    }

    // - CorpusFilter --------------------------------------------------------------------------------------------------

    @Override
    public Initializer getInitializer(Language2 language) {
        final ArrayList<CorpusFilter.Initializer> initializers = new ArrayList<>(filters.length);

        for (CorpusFilter filter : filters) {
            CorpusFilter.Initializer initializer = filter.getInitializer(language);
            if (initializer != null)
                initializers.add(initializer);
        }

        if (initializers.isEmpty()) {
            return null;
        } else {
            return new Initializer() {
                @Override
                public void onBegin() {
                    for (CorpusFilter.Initializer initializer : initializers)
                        initializer.onBegin();
                }

                @Override
                public void onLine(String line, int index) {
                    for (CorpusFilter.Initializer initializer : initializers)
                        initializer.onLine(line, index);
                }

                @Override
                public void onEnd() {
                    for (CorpusFilter.Initializer initializer : initializers)
                        initializer.onEnd();
                }
            };
        }
    }

    @Override
    public boolean accept(String line, int index) {
        for (CorpusFilter filter : filters) {
            if (!filter.accept(line, index))
                return false;
        }

        return true;
    }

    @Override
    public void clear() {
        for (CorpusFilter filter : filters)
            filter.clear();
    }

}
