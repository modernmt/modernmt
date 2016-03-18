package eu.modernmt.engine.training.cleaning;

import eu.modernmt.model.BilingualCorpus;

import java.io.IOException;

/**
 * Created by davide on 14/03/16.
 */
public interface BilingualCorpusFilter {

    interface FilterInitializer {

        void onPair(BilingualCorpus corpus, BilingualCorpus.StringPair pair) throws IOException;

    }

    FilterInitializer getInitializer();

    boolean accept(BilingualCorpus.StringPair pair) throws IOException;

}
