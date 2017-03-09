package eu.modernmt.rest.model;

import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.model.ContextVector;

/**
 * Created by davide on 21/01/16.
 */
public class TranslationResponse {

    public DecoderTranslation translation = null;
    public ContextVector context = null;
    public long session = 0;

}
