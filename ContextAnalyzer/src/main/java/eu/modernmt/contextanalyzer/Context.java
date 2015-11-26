package eu.modernmt.contextanalyzer;

import eu.modernmt.contextanalyzer.lucene.ScoreDocument;

import java.util.Collections;
import java.util.List;

/**
 * Created by davide on 10/07/15.
 */
public class Context {

    private final List<ScoreDocument> documents;
    private final double time;

    public Context(List<ScoreDocument> documents, double time) {
        this.time = time;
        this.documents = Collections.unmodifiableList(documents);
    }

    public int size() {
        return this.documents.size();
    }

    public List<ScoreDocument> getDocuments() {
        return documents;
    }

    public double getTime() {
        return time;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Result in ");
        str.append(this.time);
        str.append("s:\n");

        for (ScoreDocument doc : this.documents) {
            str.append('\t');
            str.append(doc);
            str.append('\n');
        }

        return str.substring(0, str.length() - 1);
    }
}
