package eu.modernmt.rest.model;

import eu.modernmt.context.ContextScore;
import eu.modernmt.decoder.DecoderTranslation;

import java.util.List;

/**
 * Created by davide on 21/01/16.
 */
public class TranslationResponse {

    public DecoderTranslation translation = null;
    public List<ContextScore> context = null;
    public long session = 0;

}
