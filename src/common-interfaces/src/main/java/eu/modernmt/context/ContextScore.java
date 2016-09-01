package eu.modernmt.context;

import java.io.Serializable;

/**
 * Created by davide on 27/11/15.
 */
public class ContextScore implements Serializable, Comparable<ContextScore> {

    private String id;
    private float score;

    public ContextScore(String id, float score) {
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
    public int compareTo(ContextScore o) {
        return Float.compare(score, o.score);
    }
}
