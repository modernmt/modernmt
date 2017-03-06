package eu.modernmt.cleaning.filters.draft;

import eu.modernmt.cleaning.BilingualCorpusFilter;
import eu.modernmt.model.corpus.BilingualCorpus;

import java.io.IOException;
import java.util.*;

/**
 * Created by davide on 14/03/16.
 */
public class DraftFilter implements BilingualCorpusFilter {

    private static final long MAX_TIME_BETWEEN_WORK_SESSIONS = 48L * 60L * 60L * 1000L; // 48 hours

    private HashMap<Long, ArrayList<TranslationCandidate>> candidatesMap = new HashMap<>();
    private HashMap<Long, HashSet<Integer>> filter = null;

    private static long hash(String string) {
        int length = string.length();

        String sx, dx;

        if (length > 1) {
            int hlen = length / 2;

            sx = string.substring(0, hlen);
            dx = string.substring(hlen, length);
        } else {
            sx = string;
            dx = "";
        }

        return (long) (sx.hashCode()) << 32 | (dx.hashCode()) & 0xFFFFFFFFL;
    }

    @Override
    public void onInitStart() {
        // Nothing to do
    }

    @Override
    public void onInitEnd() {
        if (filter != null)
            return;

        filter = new HashMap<>(candidatesMap.size());

        for (Map.Entry<Long, ArrayList<TranslationCandidate>> entry : candidatesMap.entrySet()) {
            ArrayList<TranslationCandidate> candidates = entry.getValue();
            Collections.sort(candidates);

            HashSet<Integer> filtered = new HashSet<>();
            TranslationCandidate lastCandidate = null;

            for (TranslationCandidate candidate : candidates) {
                if (lastCandidate == null) {
                    lastCandidate = candidate;
                } else {
                    if (candidate.timeDiff(lastCandidate) > MAX_TIME_BETWEEN_WORK_SESSIONS)
                        filtered.add(lastCandidate.getIndex());
                    lastCandidate = candidate;
                }
            }

            if (lastCandidate != null)
                filtered.add(lastCandidate.getIndex());

            filter.put(entry.getKey(), filtered);
        }

        candidatesMap.clear();
    }

    @Override
    public FilterInitializer getInitializer() {
        if (filter != null)
            return null;

        return new FilterInitializer() {

            private Date lastTimestamp = new Date(0L);

            @Override
            public void onPair(BilingualCorpus corpus, BilingualCorpus.StringPair pair, int index) throws IOException {
                String source = pair.inverted ? pair.target : pair.source;

                ArrayList<TranslationCandidate> candidates = candidatesMap.computeIfAbsent(hash(source),
                        k -> new ArrayList<>());

                Date timestamp = pair.timestamp;
                if (timestamp == null)
                    timestamp = new Date(lastTimestamp.getTime() + 60L * 1000L);
                lastTimestamp = timestamp;

                candidates.add(new TranslationCandidate(index, timestamp));
            }
        };
    }

    @Override
    public boolean accept(BilingualCorpus.StringPair pair, int index) throws IOException {
        String source = pair.inverted ? pair.target : pair.source;

        HashSet<Integer> targets = filter.get(hash(source));
        return targets != null && targets.contains(index);
    }

    @Override
    public void clear() {
        candidatesMap.clear();
        filter = null;
    }
}
