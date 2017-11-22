package eu.modernmt;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderWithNBest;
import eu.modernmt.engine.Engine;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.facade.exceptions.TranslationException;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;

import java.util.ArrayList;
import java.util.List;

public class TranslationCallable extends PriorityCallable<Translation> {

    public final LanguagePair direction;
    public final String text;
    public final ContextVector context;
    public final int nbest;

    public TranslationCallable(int priority, LanguagePair direction, String text, ContextVector context, int nbest) {
        super(priority);
        this.direction = direction;
        this.text = text;
        this.context = context;
        this.nbest = nbest;

    }
    @Override
    public Translation call() throws Exception {
        ClusterNode node = ModernMT.getNode();

        Engine engine = node.getEngine();
        Decoder decoder = engine.getDecoder();
        Preprocessor preprocessor = engine.getPreprocessor();
        Postprocessor postprocessor = engine.getPostprocessor();

        try {
            Sentence sentence = preprocessor.process(direction, text);

            Translation translation;

            if (nbest > 0) {
                DecoderWithNBest nBestDecoder = (DecoderWithNBest) decoder;
                translation = nBestDecoder.translate(direction, sentence, context, nbest);
            } else {
                translation = decoder.translate(direction, sentence, context);
            }

            // Translation
            if (!translation.hasAlignment()) {
                Aligner aligner = engine.getAligner();
                Alignment alignment = aligner.getAlignment(direction, sentence, translation);

                translation.setWordAlignment(alignment);
            }

            postprocessor.process(direction, translation);

            // NBest list
            if (translation.hasNbest()) {
                List<Translation> hypotheses = translation.getNbest();

                if (!hypotheses.get(0).hasAlignment()) {
                    ArrayList<Sentence> sources = new ArrayList<>(hypotheses.size());
                    for (int i = 0; i < hypotheses.size(); i++)
                        sources.add(sentence);

                    Aligner aligner = engine.getAligner();
                    Alignment[] alignments = aligner.getAlignments(direction, sources, hypotheses);

                    int i = 0;
                    for (Translation hypothesis : hypotheses) {
                        hypothesis.setWordAlignment(alignments[i]);
                        i++;
                    }
                }
                postprocessor.process(direction, hypotheses);
            }

            return translation;
        } catch (ProcessingException e) {
            throw new TranslationException("Problem while processing translation", e);
        } catch (AlignerException e) {
            throw new TranslationException("Problem while aligning source sentence to its translation", e);
        } catch (DecoderException e) {
            throw new TranslationException("Problem while decoding source sentence", e);
        }
    }
}
