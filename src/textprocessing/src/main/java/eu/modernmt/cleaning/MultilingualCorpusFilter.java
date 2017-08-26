package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;

/**
 * Created by davide on 14/03/16.
 */
public interface MultilingualCorpusFilter {

    interface FilterInitializer {

        void onBegin();

        void onPair(MultilingualCorpus corpus, MultilingualCorpus.StringPair pair, int index) throws IOException;

        void onEnd();
    }

    FilterInitializer getInitializer();

    boolean accept(MultilingualCorpus.StringPair pair, int index) throws IOException;

    void clear();

}
