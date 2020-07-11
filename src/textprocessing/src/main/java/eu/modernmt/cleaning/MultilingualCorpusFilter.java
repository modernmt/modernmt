package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.TranslationUnit;

public interface MultilingualCorpusFilter {

    interface Initializer {

        void onBegin();

        void onTranslationUnit(TranslationUnit tu, int index);

        void onEnd();
    }

    Initializer getInitializer();

    boolean accept(TranslationUnit tu, int index);

    void clear();

}
