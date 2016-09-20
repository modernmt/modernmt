package eu.modernmt.context;

import eu.modernmt.model.Domain;

import java.io.Serializable;

/**
 * Created by davide on 27/11/15.
 */
public class ContextScore implements Serializable, Comparable<ContextScore> {

    private Domain domain;
    private float score;

    public ContextScore(Domain domain, float score) {
        this.domain = domain;
        this.score = score;
    }

    public Domain getDomain() {
        return domain;
    }

    public float getScore() {
        return score;
    }

    @Override
    public int compareTo(ContextScore o) {
        return Float.compare(score, o.score);
    }
}
