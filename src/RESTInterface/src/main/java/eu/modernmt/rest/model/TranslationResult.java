package eu.modernmt.rest.model;

import eu.modernmt.context.ContextDocument;

import java.util.List;

/**
 * Created by davide on 30/12/15.
 */
public class TranslationResult {

    public long session = 0L;
    public String translation = null;
    public List<ContextDocument> context = null;

}
