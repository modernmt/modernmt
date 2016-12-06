package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.BilingualCorpus;

import java.io.IOException;

/**
 * Created by davide on 14/03/16.
 */
public interface BilingualCorpusFilter {

    interface FilterInitializer {

        void onPair(BilingualCorpus corpus, BilingualCorpus.StringPair pair, int index) throws IOException;

    }

    FilterInitializer getInitializer();

    void onInitStart();

    void onInitEnd();

    boolean accept(BilingualCorpus.StringPair pair, int index) throws IOException;

}
