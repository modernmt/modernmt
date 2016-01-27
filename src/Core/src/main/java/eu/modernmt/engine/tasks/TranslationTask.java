package eu.modernmt.engine.tasks;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.*;
import eu.modernmt.engine.SlaveNode;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.processing.framework.PipelineInputStream;
import eu.modernmt.processing.framework.PipelineOutputStream;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.ProcessingPipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 09/12/15.
 */
public class TranslationTask extends DistributedCallable<Translation> {

    private Sentence source;
    private List<ContextDocument> translationContext;
    private Long session;
    private boolean processing;
    private int nbest;

    public TranslationTask(Sentence source, boolean processing, int nbest) {
        this.source = source;
        this.processing = processing;
        this.nbest = nbest;
    }

    public TranslationTask(Sentence source, List<ContextDocument> translationContext, boolean processing, int nbest) {
        this.source = source;
        this.translationContext = translationContext;
        this.processing = processing;
        this.nbest = nbest;
    }

    public TranslationTask(Sentence source, TranslationSession session, boolean processing, int nbest) {
        this.source = source;
        this.session = session.getId();
        this.translationContext = session.getTranslationContext();
        this.processing = processing;
        this.nbest = nbest;
    }

    @Override
    public SlaveNode getWorker() {
        return (SlaveNode) super.getWorker();
    }

    @Override
    public Translation call() throws ProcessingException {
        SlaveNode worker = getWorker();
        Decoder decoder = worker.getDecoder();

        Sentence tokenizedSource;
        Translation translation;

        if (processing) {
            ProcessingPipeline<String, String[]> tokenizer = worker.getTokenizer();
            tokenizedSource = new Sentence(tokenizer.process(source.toString()));
        } else {
            tokenizedSource = source;
        }

        if (session != null) {
            TranslationSession session = decoder.getSession(this.session);

            if (session == null)
                if (translationContext == null)
                    throw new IllegalArgumentException("Session id is new, but no context has been provided.");
                else
                    session = decoder.openSession(this.session, translationContext);

            translation = nbest > 0 ? decoder.translate(tokenizedSource, session, nbest) : decoder.translate(tokenizedSource, session);
        } else if (translationContext != null) {
            translation = nbest > 0 ? decoder.translate(tokenizedSource, translationContext, nbest) : decoder.translate(tokenizedSource, translationContext);
        } else {
            translation = nbest > 0 ? decoder.translate(tokenizedSource, nbest) : decoder.translate(tokenizedSource);
        }

        if (processing) {
            ProcessingPipeline<String[], String> detokenizer = worker.getDetokenizer();
            translation = new Translation(detokenizer.process(translation.getTokens()), source);

            List<TranslationHypothesis> nbest = translation.getNbest();
            if (nbest != null) {
                NBestDetokenizer nBestDetokenizer = new NBestDetokenizer(nbest);
                try {
                    detokenizer.processAll(nBestDetokenizer, nBestDetokenizer);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Unexpected exception", e);
                }

                translation.setNbest(nBestDetokenizer.getResult());
            }
        }

        return translation;
    }

    private static class NBestDetokenizer implements PipelineInputStream<String[]>, PipelineOutputStream<String> {

        private List<TranslationHypothesis> source;
        private List<TranslationHypothesis> result;
        private int readIndex;
        private int writeIndex;

        public NBestDetokenizer(List<TranslationHypothesis> nbest) {
            this.source = nbest;
            this.result = new ArrayList<>(nbest.size());
            this.readIndex = 0;
            this.writeIndex = 0;
        }

        @Override
        public String[] read() {
            return source.get(readIndex++).getTokens();
        }

        @Override
        public void write(String value) {
            TranslationHypothesis original = source.get(writeIndex++);
            TranslationHypothesis hyp = new TranslationHypothesis(value, original.getTotalScore(), original.getScores());
            result.add(hyp);
        }

        @Override
        public void close() throws IOException {
        }

        public List<TranslationHypothesis> getResult() {
            return result;
        }
    }

}
