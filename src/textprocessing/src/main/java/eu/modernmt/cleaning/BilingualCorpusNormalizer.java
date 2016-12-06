package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.BilingualCorpus;

/**
 * Created by davide on 14/03/16.
 */
public interface BilingualCorpusNormalizer {

    void normalize(BilingualCorpus.StringPair pair, int index);

}
