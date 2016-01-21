package eu.modernmt.rest.model;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.Translation;

import java.util.List;

/**
 * Created by davide on 21/01/16.
 */
public class TranslationResponse {

    public Translation translation = null;
    public List<ContextDocument> context = null;
    public long session = 0;
}
