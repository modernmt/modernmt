package eu.modernmt.context;

import java.io.Serializable;

/**
 * Created by davide on 27/11/15.
 */
public class ContextDocument implements Serializable, Comparable<ContextDocument> {

    private String id;
    private float score;

    public ContextDocument(String id, float score) {
        this.id = id;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public float getScore() {
        return score;
    }

    @Override
    public int compareTo(ContextDocument o) {
        return Float.compare(score, o.score);
    }
}
