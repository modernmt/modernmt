package eu.modernmt.rest.model;

import eu.modernmt.model.ContextVector;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 02/08/17.
 */
public class ContextVectorResult {

    public final Locale source;
    public final Map<Locale, ContextVector> map;

    public ContextVectorResult(Locale source, Map<Locale, ContextVector> map) {
        this.map = map;
        this.source = source;
    }

}
