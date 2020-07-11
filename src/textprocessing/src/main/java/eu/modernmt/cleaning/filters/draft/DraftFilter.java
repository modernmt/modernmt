package eu.modernmt.cleaning.filters.draft;

import eu.modernmt.cleaning.MultilingualCorpusFilter;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.TranslationUnit;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by davide on 14/03/16.
 */
public class DraftFilter implements MultilingualCorpusFilter {

    private final HashMap<LanguageDirection, DraftFilterData> filters = new HashMap<>();
    private boolean dataReady = false;

    @Override
    public Initializer getInitializer() {
        if (dataReady)
            return null;

        return new Initializer() {

            private Date lastTimestamp = new Date(0L);

            @Override
            public void onBegin() {
                // Nothing to do
            }

            @Override
            public void onTranslationUnit(TranslationUnit tu, int index) {
                Date timestamp = tu.timestamp;
                if (timestamp == null)
                    timestamp = new Date(lastTimestamp.getTime() + 60L * 1000L);
                lastTimestamp = timestamp;

                filters.computeIfAbsent(tu.language, k -> new DraftFilterData())
                        .add(tu.source, new TranslationCandidate(index, timestamp));
            }

            @Override
            public void onEnd() {
                dataReady = true;

                for (DraftFilterData filter : filters.values())
                    filter.compile();
            }
        };
    }

    @Override
    public boolean accept(TranslationUnit tu, int index) {
        return filters.get(tu.language).accept(tu.source, index);
    }

    @Override
    public void clear() {
        for (DraftFilterData filter : filters.values())
            filter.clear();
    }

}
