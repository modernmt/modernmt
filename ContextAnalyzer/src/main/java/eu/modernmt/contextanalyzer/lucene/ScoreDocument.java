package eu.modernmt.contextanalyzer.lucene;

/**
 * Created by davide on 10/07/15.
 */
public class ScoreDocument implements Comparable<ScoreDocument> {

    public final String name;
    public float matchingScore;
    public float similarityScore;

    public ScoreDocument(String name) {
        this.name = name;
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
        StringBuilder result = new StringBuilder();
        result.append(name);
        result.append("<S:");
        result.append(similarityScore);
        result.append(", M:");
        result.append(matchingScore);
        result.append('>');
        return result.toString();
    }
}
