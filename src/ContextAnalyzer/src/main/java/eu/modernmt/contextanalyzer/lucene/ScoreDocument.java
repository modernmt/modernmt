package eu.modernmt.contextanalyzer.lucene;

import eu.modernmt.model.context.ContextDocument;

/**
 * Created by davide on 10/07/15.
 */
public class ScoreDocument implements Comparable<ScoreDocument>, ContextDocument {

    public final String name;
    public float matchingScore;
    public float similarityScore;

    public ScoreDocument(String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public int compareTo(ScoreDocument o) {
        return Float.compare(similarityScore, o.similarityScore);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScoreDocument corpusDoc = (ScoreDocument) o;

        return name.equals(corpusDoc.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name + "<S:" + similarityScore + ", M:" + matchingScore + '>';
    }

}
