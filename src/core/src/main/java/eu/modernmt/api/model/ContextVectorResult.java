package eu.modernmt.api.model;

import eu.modernmt.lang.Language2;
import eu.modernmt.model.ContextVector;

import java.util.Map;

/**
 * Created by davide on 02/08/17.
 */
public class ContextVectorResult {

    public final Language2 source;
    public final Map<Language2, ContextVector> map;
    public final boolean backwardCompatible;

    public ContextVectorResult(Language2 source, Map<Language2, ContextVector> map, boolean backwardCompatible) {
        this.map = map;
        this.source = source;
        this.backwardCompatible = backwardCompatible;
    }

}
