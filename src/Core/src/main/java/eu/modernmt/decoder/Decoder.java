package eu.modernmt.decoder;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.model.Sentence;

import java.io.Closeable;
import java.util.List;

/**
 * Created by davide on 26/11/15.
 */
public interface Decoder extends Closeable {

    // Features

    DecoderFeature[] getFeatures();

    float[] getFeatureWeights(DecoderFeature feature);

    // Translation session

    TranslationSession openSession(long id, List<ContextDocument> translationContext);

    TranslationSession getSession(long id);

    // Translate

    DecoderTranslation translate(Sentence text);

    DecoderTranslation translate(Sentence text, List<ContextDocument> translationContext);

    DecoderTranslation translate(Sentence text, TranslationSession session);

    DecoderTranslation translate(Sentence text, int nbestListSize);

    DecoderTranslation translate(Sentence text, List<ContextDocument> translationContext, int nbestListSize);

    DecoderTranslation translate(Sentence text, TranslationSession session, int nbestListSize);

}
