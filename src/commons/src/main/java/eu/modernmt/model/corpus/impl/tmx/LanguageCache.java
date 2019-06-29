package eu.modernmt.model.corpus.impl.tmx;

import eu.modernmt.lang.Language2;
import eu.modernmt.lang.LanguageDirection;
import org.apache.commons.lang3.StringUtils;

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

    private final HashMap<SKey, LanguageDirection> cache = new HashMap<>();
    private final SKey probe = new SKey();

    public LanguageDirection get(String source, String target) {
        probe.source = source;
        probe.target = target;

        LanguageDirection language = cache.get(probe);
        if (language == null) {
            language = new LanguageDirection(Language2.fromString(source), Language2.fromString(target));
            cache.put(new SKey(source, target), language);
        }

        return language;
    }
    
}
