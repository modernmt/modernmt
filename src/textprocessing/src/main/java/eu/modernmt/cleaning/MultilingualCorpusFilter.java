package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.MultilingualCorpus;

public interface MultilingualCorpusFilter {

    interface Initializer {

        void onBegin();

        void onPair(MultilingualCorpus.StringPair pair, int index);

        void onEnd();
    }

    Initializer getInitializer();

    boolean accept(MultilingualCorpus.StringPair pair, int index);

    void clear();

}
