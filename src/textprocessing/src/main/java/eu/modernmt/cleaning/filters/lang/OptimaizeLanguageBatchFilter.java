package eu.modernmt.cleaning.filters.lang;

import eu.modernmt.lang.Language;

/**
 * Created by davide on 27/12/17.
 */
public class OptimaizeLanguageBatchFilter extends AbstractOptimaizeFilter {

    private static final int MIN_SIZE = 50;
    private Blacklist blacklist = null;

    @Override
    public Initializer getInitializer() {
        return new Initializer() {

            private /* final */ OptimaizeLanguage language = null;
            private final Batch batch = new Batch();

            @Override
            public void onBegin() {
                blacklist = null;
            }

            @Override
            public void onLine(Language language, String line, int index) {
                if (this.language == null)
                    this.language = new OptimaizeLanguage(language);

                assert this.language.getLanguage().equals(language);

                if (this.language.isSupported()) {
                    batch.add(line, index);

                    if (batch.isFull())
                        analyze(batch);
                }
            }

            private void analyze(Batch batch) {
                if (batch.size() >= MIN_SIZE) {
                    String guess = guessLanguage(batch.getContent(), true);

                    if (!language.match(guess)) {
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
    public boolean accept(Language language, String line, int index) {
        return blacklist == null || !blacklist.contains(index);
    }

    @Override
    public void clear() {
        this.blacklist = null;
    }

}
