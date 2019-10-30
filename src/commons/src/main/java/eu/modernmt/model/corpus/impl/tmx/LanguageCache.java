package eu.modernmt.model.corpus.impl.tmx;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;

import java.util.HashMap;

/**
 * Created by davide on 31/07/17.
 */
class LanguageCache {

    private static class SKey {

        private Language source;
        private Language target;

        public SKey() {
        }

        public SKey(Language source, Language target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SKey sKey = (SKey) o;

            if (!source.equals(sKey.source)) return false;
            return target.equals(sKey.target);
        }

        @Override
        public int hashCode() {
            int result = source.hashCode();
            result = 31 * result + target.hashCode();
            return result;
        }

    }

    private final HashMap<SKey, LanguageDirection> directionsCache = new HashMap<>();
    private final HashMap<String, Language> languagesCache = new HashMap<>();
    private final SKey probe = new SKey();

    public LanguageDirection get(Language source, Language target) {
        probe.source = source;
        probe.target = target;

        LanguageDirection language = directionsCache.get(probe);
        if (language == null) {
            language = new LanguageDirection(source, target);
            directionsCache.put(new SKey(source, target), language);
        }

        return language;
    }

    public Language get(String text) {
        if (text == null)
            return null;

        Language language = languagesCache.get(text);
        if (language == null) {
            language = Language.fromString(text);
            languagesCache.put(text, language);
        }

        return language;
    }
    
}
