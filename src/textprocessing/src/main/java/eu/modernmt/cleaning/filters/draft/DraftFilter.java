package eu.modernmt.cleaning.filters.draft;

import eu.modernmt.cleaning.BilingualCorpusFilter;
import eu.modernmt.model.corpus.BilingualCorpus;

import java.io.IOException;
import java.util.*;

/**
 * Created by davide on 14/03/16.
 */
public class DraftFilter implements BilingualCorpusFilter {

    private static final long MAX_TIME_BETWEEN_WORK_SESSIONS = 12L * 60L * 60L * 1000L; // 12 hours

    private final HashMap<Long, ArrayList<TranslationCandidate>> candidatesMap = new HashMap<>();
    private HashMap<Long, HashSet<Long>> filter;

    @Override
    public FilterInitializer getInitializer() {
        candidatesMap.clear();
        filter = null;

        return new FilterInitializer() {

            private Date lastTimestamp = new Date(0L);

            @Override
            public void onPair(BilingualCorpus corpus, BilingualCorpus.StringPair pair) throws IOException {
                long sourceHash = TranslationCandidate.hash(pair.source);

                ArrayList<TranslationCandidate> candidates = candidatesMap.get(sourceHash);
                if (candidates == null) {
                    candidates = new ArrayList<>();
                    candidatesMap.put(sourceHash, candidates);
                }

                Date timestamp = pair.timestamp;
                if (timestamp == null)
                    timestamp = new Date(lastTimestamp.getTime() + 60L * 1000L);
                lastTimestamp = timestamp;

                candidates.add(new TranslationCandidate(pair.target, lastTimestamp));
            }
        };
    }

    private HashMap<Long, HashSet<Long>> computeFilter() {
        HashMap<Long, HashSet<Long>> result = new HashMap<>(candidatesMap.size());

        for (Map.Entry<Long, ArrayList<TranslationCandidate>> entry : candidatesMap.entrySet()) {
            ArrayList<TranslationCandidate> candidates = entry.getValue();
            Collections.sort(candidates);

            HashSet<Long> filtered = new HashSet<>();
            TranslationCandidate lastCandidate = null;

            for (TranslationCandidate candidate : candidates) {
                if (lastCandidate == null) {
                    lastCandidate = candidate;
                } else {
                    if (candidate.timeDiff(lastCandidate) > MAX_TIME_BETWEEN_WORK_SESSIONS)
                        filtered.add(lastCandidate.getHash());
                    lastCandidate = candidate;
                }
            }

            if (lastCandidate != null)
                filtered.add(lastCandidate.getHash());

            result.put(entry.getKey(), filtered);
        }

        candidatesMap.clear();

        return result;
    }

    @Override
    public boolean accept(BilingualCorpus.StringPair pair) throws IOException {
        if (filter == null) {
            synchronized (this) {
                if (filter == null)
                    filter = computeFilter();
            }
        }

        long sourceHash = TranslationCandidate.hash(pair.source);

        HashSet<Long> targets = filter.get(sourceHash);
        if (targets == null)
            return false;

        long targetHash = TranslationCandidate.hash(pair.target);

        if (targets.contains(targetHash)) {
            targets.remove(targetHash);

            if (targets.isEmpty())
                filter.remove(sourceHash);

            return true;
        } else {
            return false;
        }
    }
}
