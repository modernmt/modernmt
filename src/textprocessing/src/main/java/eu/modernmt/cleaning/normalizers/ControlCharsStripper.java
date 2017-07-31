package eu.modernmt.cleaning.normalizers;

import eu.modernmt.cleaning.BilingualCorpusNormalizer;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.processing.chars.ControlCharsRemover;

/**
 * Created by davide on 17/11/16.
 */
public class ControlCharsStripper implements BilingualCorpusNormalizer {

    @Override
    public void normalize(MultilingualCorpus.StringPair pair, int index) {
        pair.source = ControlCharsRemover.strip(pair.source);
        pair.target = ControlCharsRemover.strip(pair.target);
    }

}
