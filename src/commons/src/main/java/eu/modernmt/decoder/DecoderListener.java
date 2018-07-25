package eu.modernmt.decoder;

import eu.modernmt.lang.LanguagePair;

import java.util.Set;

/**
 * Created by davide on 02/08/17.
 */
public interface DecoderListener {

    void onTranslationDirectionsChanged(Set<LanguagePair> directions);

    void onDecoderAvailabilityChanged(int availability);

}
