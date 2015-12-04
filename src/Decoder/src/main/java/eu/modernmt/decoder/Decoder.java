package eu.modernmt.decoder;

import eu.modernmt.context.ContextDocument;

import java.io.Closeable;
import java.util.List;

/**
 * Created by davide on 26/11/15.
 */
public interface Decoder extends Closeable {

    // Translation session

    TranslationSession openSession(List<ContextDocument> translationContext);

    TranslationSession getSession(long id);

    // Translate

    Translation translate(Sentence text);

    Translation translate(Sentence text, List<ContextDocument> translationContext);

    Translation translate(Sentence text, TranslationSession session);

    List<TranslationHypothesis> translate(Sentence text, int nbestListSize);

    List<TranslationHypothesis> translate(Sentence text, List<ContextDocument> translationContext, int nbestListSize);

    List<TranslationHypothesis> translate(Sentence text, TranslationSession session, int nbestListSize);


}
