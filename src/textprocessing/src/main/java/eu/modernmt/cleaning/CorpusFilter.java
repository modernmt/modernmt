package eu.modernmt.cleaning;

import eu.modernmt.lang.Language;

public interface CorpusFilter {

    interface Initializer {

        void onBegin();

        void onLine(Language language, String line, int index);

        void onEnd();
    }

    Initializer getInitializer();

    boolean accept(Language language, String line, int index);

    void clear();

}
