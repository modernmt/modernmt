package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.MultilingualCorpus;

import java.util.ArrayList;

public class ChainedMultilingualCorpusFilter implements MultilingualCorpusFilter, CorpusNormalizer {

    public static class Builder {
        private final ArrayList<MultilingualCorpusFilter> filters = new ArrayList<>();
        private final ArrayList<CorpusNormalizer> normalizers = new ArrayList<>();

        public void add(MultilingualCorpusFilter filter) {
            this.filters.add(filter);
        }

        public void add(CorpusNormalizer normalizer) {
            this.normalizers.add(normalizer);
        }

        public ChainedMultilingualCorpusFilter build() {
            MultilingualCorpusFilter[] filters = this.filters.toArray(new MultilingualCorpusFilter[0]);
            CorpusNormalizer[] normalizers = this.normalizers.toArray(new CorpusNormalizer[0]);
            return new ChainedMultilingualCorpusFilter(normalizers, filters);
        }
    }

    private final CorpusNormalizer[] normalizers;
    private final MultilingualCorpusFilter[] filters;

    private ChainedMultilingualCorpusFilter(CorpusNormalizer[] normalizers, MultilingualCorpusFilter[] filters) {
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

    public void normalize(MultilingualCorpus.StringPair pair) {
        pair.source = normalize(pair.source);
        pair.target = normalize(pair.target);
    }

    // - CorpusFilter --------------------------------------------------------------------------------------------------

    @Override
    public Initializer getInitializer() {
        final ArrayList<Initializer> initializers = new ArrayList<>(filters.length);

        for (MultilingualCorpusFilter filter : filters) {
            Initializer initializer = filter.getInitializer();
            if (initializer != null)
                initializers.add(initializer);
        }

        if (initializers.isEmpty()) {
            return null;
        } else {
            return new Initializer() {
                @Override
                public void onBegin() {
                    for (Initializer initializer : initializers)
                        initializer.onBegin();
                }

                @Override
                public void onPair(MultilingualCorpus.StringPair pair, int index) {
                    for (Initializer initializer : initializers)
                        initializer.onPair(pair, index);
                }

                @Override
                public void onEnd() {
                    for (Initializer initializer : initializers)
                        initializer.onEnd();
                }
            };
        }
    }

    @Override
    public boolean accept(MultilingualCorpus.StringPair pair, int index) {
        for (MultilingualCorpusFilter filter : filters) {
            if (!filter.accept(pair, index))
                return false;
        }

        return true;
    }

    @Override
    public void clear() {
        for (MultilingualCorpusFilter filter : filters)
            filter.clear();
    }

}
