package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.MultilingualCorpus;

/**
 * Created by davide on 14/03/16.
 */
public interface MultilingualCorpusNormalizer {

    void normalize(MultilingualCorpus.StringPair pair, int index);

}
