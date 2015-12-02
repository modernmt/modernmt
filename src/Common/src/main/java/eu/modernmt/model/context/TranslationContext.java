package eu.modernmt.model.context;

import java.util.List;

/**
 * Created by davide on 27/11/15.
 */
public interface TranslationContext {

    int size();

    List<? extends ContextDocument> getDocuments();

    float getScore(ContextDocument document);

}
