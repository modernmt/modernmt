package eu.modernmt.cleaning.filters.lang;

import eu.modernmt.lang.Language;

public class OptimaizeLanguage {

    private final Language language;
    private final String key;
    private final boolean supported;

    public OptimaizeLanguage(Language language) {
        this.language = language;
        this.key = makeLanguageKey(language.getLanguage());
        this.supported = AbstractOptimaizeFilter.isSupported(language);
    }

    public Language getLanguage() {
        return language;
    }

    public boolean isSupported() {
        return supported;
    }

    public boolean match(Language language) {
        return match(language.getLanguage());
    }

    public boolean match(String language) {
        if (language == null)
            return false;

        String key = makeLanguageKey(language);
        return this.key.equals(key);
    }

    private String makeLanguageKey(String language) {
        // We cannot rely on identification of these languages
        if ("sr".equalsIgnoreCase(language) || "hr".equalsIgnoreCase(language) || "bs".equalsIgnoreCase(language)) {
            return "sr_hr_bs";
        } else if ("id".equalsIgnoreCase(language) || "ms".equalsIgnoreCase(language)) {
            return "id_ms";
        } else {
            return language.toLowerCase();
        }
    }

}
