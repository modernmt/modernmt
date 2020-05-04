package eu.modernmt.cleaning.filters.lang;

import eu.modernmt.lang.Language;

/**
 * Created by davide on 27/12/17.
 */
public class OptimaizeLanguageFilter extends AbstractOptimaizeFilter {

    private /* final */ OptimaizeLanguage language = null;

    @Override
    public Initializer getInitializer() {
        return null;
    }

    @Override
    public boolean accept(Language language, String line, int index) {
        if (this.language == null)
            this.language = new OptimaizeLanguage(language);

        assert this.language.getLanguage().equals(language);

        if (this.language.isSupported()) {
            String guess = guessLanguage(line, false);
            return this.language.match(guess);
        } else {
            return true;
        }
    }

    @Override
    public void clear() {
        language = null;
    }

}
