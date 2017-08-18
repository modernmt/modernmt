package eu.modernmt.cleaning.filters.draft;

import java.util.*;

/**
 * Created by davide on 31/07/17.
 */
class DraftFilterData {

    private static final long MAX_TIME_BETWEEN_WORK_SESSIONS = 48L * 60L * 60L * 1000L; // 48 hours

    private HashMap<Long, ArrayList<TranslationCandidate>> candidatesMap = new HashMap<>();
    private HashMap<Long, HashSet<Integer>> filter = null;

    public void add(String source, TranslationCandidate candidate) {
        candidatesMap.computeIfAbsent(hash(source), k -> new ArrayList<>())
                .add(candidate);
    }

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

    void compile() {
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

    boolean accept(String source, int index) {
        HashSet<Integer> targets = filter.get(hash(source));
        return targets != null && targets.contains(index);
    }

    void clear() {
        candidatesMap.clear();
        filter = null;
    }
}
