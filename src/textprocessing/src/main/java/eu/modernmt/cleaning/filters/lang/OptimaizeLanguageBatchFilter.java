package eu.modernmt.cleaning.filters.lang;

import eu.modernmt.lang.Language;

/**
 * Created by davide on 27/12/17.
 */
public class OptimaizeLanguageBatchFilter extends AbstractOptimaizeFilter {

    private static final int MIN_SIZE = 50;
    private Blacklist blacklist = null;

    @Override
    public Initializer getInitializer(Language language) {
        boolean isLanguageSupported = isSupported(language);
        String languageKey = makeLanguageKey(language.getLanguage());

        return new Initializer() {

            private final Batch batch = new Batch();

            @Override
            public void onBegin() {
                blacklist = null;
            }

            @Override
            public void onLine(String line, int index) {
                if (isLanguageSupported) {
                    batch.add(line, index);

                    if (batch.isFull())
                        analyze(batch);
                }
            }

            private void analyze(Batch batch) {
                if (batch.size() >= MIN_SIZE) {
                    String lang = guessLanguage(batch.getContent(), true);
                    String langKey = makeLanguageKey(lang);

                    if (!languageKey.equals(langKey)) {
                        int beginIndex = batch.getBeginIndex();
                        int endIndex = batch.getEndIndex();

                        if (blacklist == null)
                            blacklist = new Blacklist();

                        blacklist.add(beginIndex, endIndex);
                    }
                }

                batch.clear();
            }

            @Override
            public void onEnd() {
                if (!batch.isEmpty())
                    analyze(batch);
            }
        };
    }

    @Override
    public boolean accept(String line, int index) {
        return blacklist == null || !blacklist.contains(index);
    }

    @Override
    public void clear() {
        this.blacklist = null;
    }

}
