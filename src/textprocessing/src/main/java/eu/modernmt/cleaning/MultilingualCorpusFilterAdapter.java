package eu.modernmt.cleaning;


import eu.modernmt.lang.Language2;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.util.HashMap;

public class MultilingualCorpusFilterAdapter implements MultilingualCorpusFilter {

    public interface Factory {

        CorpusFilter create();

    }

    private final Factory factory;
    private final boolean hasInitializer;
    private final HashMap<Language2, CorpusFilter> filters = new HashMap<>();

    public MultilingualCorpusFilterAdapter(Class<? extends CorpusFilter> clazz) {
        this(() -> {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    public MultilingualCorpusFilterAdapter(Factory factory) {
        this.factory = factory;
        this.hasInitializer = (factory.create().getInitializer(Language2.ENGLISH) != null);
    }

    @Override
    public Initializer getInitializer() {
        if (!hasInitializer)
            return null;

        HashMap<Language2, CorpusFilter.Initializer> initializers = new HashMap<>();

        return new Initializer() {
            @Override
            public void onBegin() {
                // Nothing to do
            }

            @Override
            public void onPair(MultilingualCorpus.StringPair pair, int index) {
                initializers.computeIfAbsent(pair.language.source, this::createInitializer)
                        .onLine(pair.source, index);
                initializers.computeIfAbsent(pair.language.target, this::createInitializer)
                        .onLine(pair.target, index);
            }

            private CorpusFilter.Initializer createInitializer(Language2 language) {
                CorpusFilter filter = filters.computeIfAbsent(language, (l) -> factory.create());
                CorpusFilter.Initializer initializer = filter.getInitializer(language);
                initializer.onBegin();

                return initializer;
            }

            @Override
            public void onEnd() {
                for (CorpusFilter.Initializer initializer : initializers.values())
                    initializer.onEnd();
            }
        };
    }

    @Override
    public boolean accept(MultilingualCorpus.StringPair pair, int index) {
        CorpusFilter sourceFilter = filters.computeIfAbsent(pair.language.source, (l) -> factory.create());
        CorpusFilter targetFilter = filters.computeIfAbsent(pair.language.target, (l) -> factory.create());

        return sourceFilter.accept(pair.source, index) && targetFilter.accept(pair.target, index);
    }

    @Override
    public void clear() {
        for (CorpusFilter filter : filters.values())
            filter.clear();
    }
}
