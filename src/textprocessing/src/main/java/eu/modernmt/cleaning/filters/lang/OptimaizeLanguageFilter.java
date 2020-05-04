package eu.modernmt.cleaning.filters.lang;

import eu.modernmt.lang.Language;

/**
 * Created by davide on 27/12/17.
 */
public class OptimaizeLanguageFilter extends AbstractOptimaizeFilter {

    private String languageKey = null;

    @Override
    public Initializer getInitializer(Language language) {
        if (isSupported(language))
            languageKey = makeLanguageKey(language.getLanguage());
        else
            languageKey = null;

        return null;
    }

    @Override
    public boolean accept(String line, int index) {
        if (languageKey != null) {
            String lang = guessLanguage(line, false);
            String langKey = makeLanguageKey(lang);

            return languageKey.equals(langKey);
        } else {
            return true;
        }
    }

    @Override
    public void clear() {
        languageKey = null;
    }

}
