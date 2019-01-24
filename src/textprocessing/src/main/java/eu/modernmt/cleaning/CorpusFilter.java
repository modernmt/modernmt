package eu.modernmt.cleaning;

import eu.modernmt.lang.Language;

public interface CorpusFilter {

    interface Initializer {

        void onBegin();

        void onLine(String line, int index);

        void onEnd();
    }

    Initializer getInitializer(Language language);

    boolean accept(String line, int index);

    void clear();

}
