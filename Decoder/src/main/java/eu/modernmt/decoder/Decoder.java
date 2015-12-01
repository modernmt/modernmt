package eu.modernmt.decoder;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.context.TranslationContext;

import java.io.Closeable;
import java.util.List;

/**
 * Created by davide on 26/11/15.
 */
public interface Decoder extends Closeable {

    List<DecoderFeature> getFeatureWeights();

    void setFeatureWeights(List<DecoderFeature> features);

    DecoderSession openSession(TranslationContext translationContext);

    Translation translate(Sentence text);

    Translation translate(Sentence text, TranslationContext translationContext);

    Translation translate(Sentence text, DecoderSession session);

    Translation translate(Sentence text, DecoderSession session, int nbestListSize);


}
