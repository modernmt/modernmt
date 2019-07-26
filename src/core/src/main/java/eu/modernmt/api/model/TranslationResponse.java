package eu.modernmt.api.model;

import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Priority;
import eu.modernmt.model.Translation;

/**
 * Created by davide on 21/01/16.
 */
public class TranslationResponse {

    public Translation translation = null;
    public ContextVector context = null;
    public boolean verbose = false;
    public final Priority priority;

    private final long creationTimestamp = System.currentTimeMillis();

    public TranslationResponse(Priority priority) {
        this.priority = priority;
    }

    public long getTotalTime() {
        return System.currentTimeMillis() - creationTimestamp;
    }

}
