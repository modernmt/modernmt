package eu.modernmt.lang;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by davide on 31/07/17.
 */
public class LanguageIndex implements Iterable<LanguagePair> {

    private static final LanguagePair NONE = new LanguagePair(null, null);

    private final Set<LanguagePair> languages;
    private final ConcurrentHashMap<LanguagePair, LanguagePair> cache = new ConcurrentHashMap<>();

    public LanguageIndex(LanguagePair... languages) {
        this(Arrays.asList(languages));
    }

    public LanguageIndex(Collection<LanguagePair> languages) {
        this.languages = Collections.unmodifiableSet(new HashSet<>(languages));
    }

    public Set<LanguagePair> getLanguages() {
        return languages;
    }

    public int size() {
        return languages.size();
    }

    public boolean isSupported(LanguagePair pair) {
        return pair != null && languages.contains(pair);
    }

    public LanguagePair map(LanguagePair pair) {
        LanguagePair mapped = strictMap(pair);

        if (mapped == null) {
            mapped = strictMap(pair.reversed());

            if (mapped != null)
                mapped = mapped.reversed();
        }

        return mapped;
    }

    private LanguagePair strictMap(LanguagePair pair) {
        if (languages.contains(pair))
            return pair;

        // Search in mapping cache
        LanguagePair output = cache.get(pair);
        if (output != null)
            return output == NONE ? null : output;

        Locale reducedSource = new Locale(pair.source.getLanguage());
        LanguagePair reduced;

        // Test language pair with reduced source language
        // Before: en-GB > pt-BR
        // After:  en > pt-BR
        reduced = new LanguagePair(reducedSource, pair.target);
        if (languages.contains(reduced)) {
            cache.put(pair, reduced);
            return reduced;
        }

        Locale reducedTarget = new Locale(pair.target.getLanguage());

        // Test language pair with reduced target language
        // Before: en-GB > pt-BR
        // After:  en-GB > pt
        reduced = new LanguagePair(pair.source, reducedTarget);
        if (languages.contains(reduced)) {
            cache.put(pair, reduced);
            return reduced;
        }

        // Test language pair with reduced source and target languages
        // Before: en-GB > pt-BR
        // After:  en > pt
        reduced = new LanguagePair(reducedSource, reducedTarget);
        if (languages.contains(reduced)) {
            cache.put(pair, reduced);
            return reduced;
        } else {
            cache.put(pair, NONE);
            return null;
        }
    }

    @Override
    public Iterator<LanguagePair> iterator() {
        return languages.iterator();
    }

}
