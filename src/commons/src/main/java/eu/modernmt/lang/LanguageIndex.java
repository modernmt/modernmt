package eu.modernmt.lang;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by davide on 31/07/17.
 */
public class LanguageIndex implements Iterable<LanguagePair> {

    private final Set<LanguagePair> languages;
    private final HashMap<LanguageKey, HashSet<LanguagePair>> index;
    private final ConcurrentHashMap<LanguagePair, List<LanguagePair>> cache = new ConcurrentHashMap<>();

    public LanguageIndex(LanguagePair... languages) {
        this(Arrays.asList(languages));
    }

    public LanguageIndex(Collection<LanguagePair> languages) {
        this.languages = Collections.unmodifiableSet(new HashSet<>(languages));

        this.index = new HashMap<>(languages.size());
        for (LanguagePair language : languages) {
            LanguageKey key = LanguageKey.fromLanguage(language);
            HashSet<LanguagePair> entry = this.index.computeIfAbsent(key, (k) -> new HashSet<>(4));
            entry.add(language);
        }
    }

    public Set<LanguagePair> getLanguages() {
        return languages;
    }

    public int size() {
        return languages.size();
    }

    public boolean contains(LanguagePair pair) {
        return pair != null && languages.contains(pair);
    }

    public boolean match(LanguagePair pair) {
        return !map(pair).isEmpty();
    }

    public LanguagePair mapToBestMatching(LanguagePair pair) {
        if (languages.contains(pair) || languages.contains(pair.reversed()))
            return pair;

        List<LanguagePair> mapping = map(pair);
        return mapping.isEmpty() ? null : mapping.get(0);
    }

    public List<LanguagePair> map(LanguagePair pair) {
        List<LanguagePair> result = cache.get(pair);

        if (result != null)
            return result;

        Set<LanguagePair> set;
        set = map(pair, null, false);
        set = map(pair.reversed(), set, true);

        if (set != null) {
            result = new ArrayList<>(set);
            result.sort((a, b) -> {
                int cmp = a.source.compareTo(b.source);
                return -(cmp == 0 ? a.target.compareTo(b.target) : cmp);
            });

            result = Collections.unmodifiableList(result);
            cache.put(pair, result);
        }

        return result == null ? Collections.emptyList() : result;
    }

    private Set<LanguagePair> map(LanguagePair pair, Set<LanguagePair> result, boolean reverse) {
        LanguageKey key = LanguageKey.fromLanguage(pair);
        Set<LanguagePair> mappings = this.index.get(key);

        if (mappings != null) {
            for (LanguagePair mapping : mappings) {
                if (match(pair, mapping)) {
                    if (result == null)
                        result = new HashSet<>(4);

                    result.add(reverse ? mapping.reversed() : mapping);
                }
            }
        }

        return result;
    }

    private static boolean match(LanguagePair a, LanguagePair b) {
        return match(a.source, b.source) && match(a.target, b.target);
    }

    private static boolean match(Language a, Language b) {
        if (!a.getLanguage().equals(b.getLanguage()))
            return false;

        String aRegion = a.getRegion();
        String bRegion = b.getRegion();

        return aRegion == null || bRegion == null || aRegion.equals(bRegion);
    }

    /**
     * If this LanguageIndex only supports one LanguagePair, this method returns it.
     * Else, it returns null.
     *
     * @return the language pair, if only one is supported; null otherwise
     */
    public LanguagePair asSingleLanguagePair() {
        return languages.size() == 1 ? languages.iterator().next() : null;
    }

    @Override
    public Iterator<LanguagePair> iterator() {
        return languages.iterator();
    }

    private static final class LanguageKey {

        private final String source;
        private final String target;

        public static LanguageKey fromLanguage(LanguagePair language) {
            return new LanguageKey(language.source.getLanguage(), language.target.getLanguage());
        }

        private LanguageKey(String source, String target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LanguageKey that = (LanguageKey) o;

            if (!source.equals(that.source)) return false;
            return target.equals(that.target);
        }

        @Override
        public int hashCode() {
            int result = source.hashCode();
            result = 31 * result + target.hashCode();
            return result;
        }
    }

}
