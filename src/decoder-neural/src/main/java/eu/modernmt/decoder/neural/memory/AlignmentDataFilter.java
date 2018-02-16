package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.LanguagePair;

import java.util.HashMap;
import java.util.Map;

public class AlignmentDataFilter implements TranslationMemory.DataFilter {

    private final HashMap<LanguageKey, Float> thresholds;

    public AlignmentDataFilter(Map<LanguagePair, Float> thresholds) {
        this.thresholds = new HashMap<>(thresholds.size() * 2);

        for (Map.Entry<LanguagePair, Float> entry : thresholds.entrySet()) {
            LanguagePair language = entry.getKey();
            Float threshold = entry.getValue();

            if (language.source.getRegion() != null || language.target.getRegion() != null)
                throw new IllegalArgumentException("Cannot specify region for Alignment filter: " + language);

            LanguageKey key = LanguageKey.parse(language);
            this.thresholds.put(key, threshold);
            this.thresholds.put(key.reversed(), threshold);
        }
    }

    @Override
    public boolean accept(TranslationUnit unit) {
        if (unit.alignment == null)
            return true;

        Float threshold = thresholds.get(LanguageKey.parse(unit.direction));
        return threshold == null || unit.alignment.getScore() >= threshold;
    }

    private static final class LanguageKey {

        public static LanguageKey parse(LanguagePair pair) {
            return new LanguageKey(pair.source.getLanguage(), pair.target.getLanguage());
        }

        private final String source;
        private final String target;

        public LanguageKey(String source, String target) {
            this.source = source;
            this.target = target;
        }

        public LanguageKey reversed() {
            return new LanguageKey(target, source);
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
