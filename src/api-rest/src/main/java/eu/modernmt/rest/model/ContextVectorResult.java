package eu.modernmt.rest.model;

import eu.modernmt.lang.Language;
import eu.modernmt.model.ContextVector;

import java.util.Map;

/**
 * Created by davide on 02/08/17.
 */
public class ContextVectorResult {

    public final Language source;
    public final Map<Language, ContextVector> map;
    public final boolean backwardCompatible;

    public ContextVectorResult(Language source, Map<Language, ContextVector> map, boolean backwardCompatible) {
        this.map = map;
        this.source = source;
        this.backwardCompatible = backwardCompatible;
    }

}
