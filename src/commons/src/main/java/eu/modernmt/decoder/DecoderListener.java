package eu.modernmt.decoder;

import eu.modernmt.lang.LanguageDirection;

import java.util.Set;

/**
 * Created by davide on 02/08/17.
 */
public interface DecoderListener {

    void onTranslationDirectionsChanged(Set<LanguageDirection> directions);

    void onDecoderAvailabilityChanged(int currentAvailability, int maxAvailability);

}
