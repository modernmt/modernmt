package eu.modernmt.facade;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderWithNBest;
import eu.modernmt.engine.Engine;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Created by davide on 21/04/16.
 */
class TranslateOperation implements Callable<Translation>, Serializable {

    public final String text;
    public final ContextVector context;
    public final int nbest;

    public TranslateOperation(String text, ContextVector context, int nbest) {
        this.text = text;
        this.context = context;
        this.nbest = nbest;
    }

    @Override
    public Translation call() throws ProcessingException, DecoderException, AlignerException {
        ClusterNode node = ModernMT.getNode();

        Engine engine = node.getEngine();
        Decoder decoder = engine.getDecoder();
        Preprocessor preprocessor = engine.getSourcePreprocessor();
        Postprocessor postprocessor = engine.getPostprocessor();

        Sentence sentence = preprocessor.process(text);

        Translation translation;

        if (nbest > 0) {
            DecoderWithNBest nBestDecoder = (DecoderWithNBest) decoder;
            translation = nBestDecoder.translate(sentence, context, nbest);
        } else {
            translation = decoder.translate(sentence, context);
        }

        if (!translation.hasAlignment()) {
            Aligner aligner = engine.getAligner();
            Alignment alignment = aligner.getAlignment(sentence, translation);

            translation.setAlignment(alignment);
        }

        postprocessor.process(translation);

        return translation;
    }
}