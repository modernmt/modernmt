package eu.modernmt.cleaning.normalizers;

import eu.modernmt.cleaning.CorpusNormalizer;
import eu.modernmt.processing.normalizers.ControlCharsRemover;

/**
 * Created by davide on 17/11/16.
 */
public class ControlCharsStripper implements CorpusNormalizer {

    @Override
    public String normalize(String line) {
        return ControlCharsRemover.strip(line);
    }
}
