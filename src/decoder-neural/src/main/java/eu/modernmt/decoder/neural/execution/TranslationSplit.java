package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

public class TranslationSplit {

    public final Sentence sentence;
    public final ScoreEntry[] suggestions;
    private Translation translation;
    private Throwable exception;
    private Scheduler.TranslationLock lock;

    public TranslationSplit(Sentence sentence, ScoreEntry[] suggestions) {
        this.sentence = sentence;
        this.suggestions = suggestions;
    }

    public void setLock(Scheduler.TranslationLock lock) {
        this.lock = lock;
    }

    public Translation getTranslation() throws DecoderException {
        if (exception == null)
            return translation;
        else if (exception instanceof RuntimeException)
            throw (RuntimeException) exception;
        else if (exception instanceof DecoderException)
            throw (DecoderException) exception;
        else
            throw new DecoderException("Unexpected error: " + exception.getMessage(), exception);
    }

    public void setTranslation(Translation translation) {
        this.translation = translation;
        if (this.lock != null)
            this.lock.translationSplitCompleted(this);
    }

    public void setException(Throwable exception) {
        this.exception = exception;
        if (this.lock != null)
            this.lock.translationSplitCompleted(this);
    }
}
