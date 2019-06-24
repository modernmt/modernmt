package eu.modernmt.model.corpus.impl.tmx;

import eu.modernmt.lang.Language;
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
            language = new LanguageDirection(
                    Language.fromString(normalize(source)),
                    Language.fromString(normalize(target)));
            cache.put(new SKey(source, target), language);
        }

        return language;
    }

    // Some TMXs have noisy language tas; i.e. en_US, EN-US, EN ecc..
    private static String normalize(String languageTag) {
        languageTag = languageTag.replace('_', '-');

        if (languageTag.indexOf('-') != -1) {
            String[] parts = languageTag.split("-");

            parts[0] = parts[0].toLowerCase(); // language code
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];

                switch (part.length()) {
                    case 4: // script code
                        part = Character.toUpperCase(part.charAt(0)) + part.substring(1);
                        break;
                    case 2: // ISO 3166-1 Geo Code
                        part = part.toUpperCase();
                        break;

                }

                parts[i] = part;
            }

            languageTag = StringUtils.join(parts, '-');
        } else {
            languageTag = languageTag.toLowerCase();
        }

        return languageTag;
    }
    
}
