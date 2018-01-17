package eu.modernmt.model.corpus.impl.tmx;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;

import java.util.HashMap;

/**
 * Created by davide on 31/07/17.
 */
class LanguageCache {

    private static class SKey {

        private String source;
        private String target;

        public SKey() {
        }

        public SKey(String source, String target) {
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

    private final HashMap<SKey, LanguagePair> cache = new HashMap<>();
    private final SKey probe = new SKey();

    public LanguagePair get(String source, String target) {
        probe.source = source;
        probe.target = target;

        LanguagePair language = cache.get(probe);
        if (language == null) {
            language = new LanguagePair(Language.fromString(source), Language.fromString(target));
            cache.put(new SKey(source, target), language);
        }

        return language;
    }

}
