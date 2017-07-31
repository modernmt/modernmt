package eu.modernmt.cleaning.filters.draft;

import eu.modernmt.cleaning.BilingualCorpusFilter;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by davide on 14/03/16.
 */
public class DraftFilter implements BilingualCorpusFilter {

    private final HashMap<LanguagePair, DraftFilterData> filters = new HashMap<>();
    private boolean dataReady = false;

    @Override
    public void onInitStart() {
        // Nothing to do
    }

    @Override
    public void onInitEnd() {
        if (dataReady)
            return;

        dataReady = true;

        for (DraftFilterData filter : filters.values())
            filter.compile();
    }

    @Override
    public FilterInitializer getInitializer() {
        if (dataReady)
            return null;

        return new FilterInitializer() {

            private Date lastTimestamp = new Date(0L);

            @Override
            public void onPair(MultilingualCorpus corpus, MultilingualCorpus.StringPair pair, int index) throws IOException {
                Date timestamp = pair.timestamp;
                if (timestamp == null)
                    timestamp = new Date(lastTimestamp.getTime() + 60L * 1000L);
                lastTimestamp = timestamp;

                filters.computeIfAbsent(pair.language, k -> new DraftFilterData())
                        .add(pair.source, new TranslationCandidate(index, timestamp));
            }
        };
    }

    @Override
    public boolean accept(MultilingualCorpus.StringPair pair, int index) throws IOException {
        return filters.get(pair.language).accept(pair.source, index);
    }

    @Override
    public void clear() {
        for (DraftFilterData filter : filters.values())
            filter.clear();
    }

}
