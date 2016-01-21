package eu.modernmt.decoder;

import eu.modernmt.context.ContextDocument;

import java.io.Closeable;
import java.util.List;

/**
 * Created by davide on 26/11/15.
 */
public interface Decoder extends Closeable {

    // Translation session

    TranslationSession openSession(long id, List<ContextDocument> translationContext);

    TranslationSession getSession(long id);

    // Translate

    Translation translate(Sentence text);

    Translation translate(Sentence text, List<ContextDocument> translationContext);

    Translation translate(Sentence text, TranslationSession session);

    Translation translate(Sentence text, int nbestListSize);

    Translation translate(Sentence text, List<ContextDocument> translationContext, int nbestListSize);

    Translation translate(Sentence text, TranslationSession session, int nbestListSize);

}
