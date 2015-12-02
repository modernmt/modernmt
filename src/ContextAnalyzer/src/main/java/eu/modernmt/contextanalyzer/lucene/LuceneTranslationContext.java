package eu.modernmt.contextanalyzer.lucene;

import eu.modernmt.model.context.ContextDocument;
import eu.modernmt.model.context.TranslationContext;

import java.util.List;

/**
 * Created by davide on 27/11/15.
 */
public class LuceneTranslationContext implements TranslationContext {

    private List<ScoreDocument> documents;

    public LuceneTranslationContext(List<ScoreDocument> documents) {
        this.documents = documents;
    }

    @Override
    public int size() {
        return this.documents.size();
    }

    @Override
    public List<? extends ContextDocument> getDocuments() {
        return this.documents;
    }

    @Override
    public float getScore(ContextDocument document) {
        int index = this.documents.indexOf(document);

        if (index < 0)
            return 0.f;
        else
            return this.documents.get(index).similarityScore;
    }
}
