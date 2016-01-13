package eu.modernmt.engine.tasks;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.Sentence;
import eu.modernmt.decoder.Translation;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.engine.MMTWorker;
import eu.modernmt.engine.TranslationEngine;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.tokenizer.DetokenizerPool;
import eu.modernmt.tokenizer.TokenizerPool;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by davide on 09/12/15.
 */
public class TranslationTask extends DistributedCallable<String> {

    private String text;
    private List<ContextDocument> translationContext;
    private Long session;
    private boolean processing;

    public TranslationTask(String text, boolean processing) {
        this.text = text;
        this.processing = processing;
    }

    public TranslationTask(String text, List<ContextDocument> translationContext, boolean processing) {
        this.text = text;
        this.translationContext = translationContext;
        this.processing = processing;
    }

    public TranslationTask(String text, TranslationSession session, boolean processing) {
        this.text = text;
        this.session = session.getId();
        this.translationContext = session.getTranslationContext();
        this.processing = processing;
    }

    @Override
    public MMTWorker getWorker() {
        return (MMTWorker) super.getWorker();
    }

    @Override
    public String call() throws IOException {
        MMTWorker worker = getWorker();
        Decoder decoder = worker.getDecoder();

        Sentence sentence;
        Translation translation;

        if (processing) {
            TokenizerPool tokenizer = worker.getTokenizer();
            String[] tokens = tokenizer.tokenize(Collections.singletonList(text)).get(0);
            sentence = new Sentence(tokens);
        } else {
            sentence = new Sentence(text);
        }

        System.out.println(sentence);

        if (session != null) {
            TranslationSession session = decoder.getSession(this.session);

            if (session == null)
                if (translationContext == null)
                    throw new IllegalArgumentException("Session id is new, but no context has been provided.");
                else
                    session = decoder.openSession(this.session, translationContext);

            translation = decoder.translate(sentence, session);
        } else if (translationContext != null) {
            translation = decoder.translate(sentence, translationContext);
        } else {
            translation = decoder.translate(sentence);
        }

        if (processing) {
            DetokenizerPool detokenizer = worker.getDetokenizer();
            return detokenizer.detokenize(Collections.singletonList(translation.getTokens())).get(0);
        } else {
            return translation.toString();
        }
    }

}
