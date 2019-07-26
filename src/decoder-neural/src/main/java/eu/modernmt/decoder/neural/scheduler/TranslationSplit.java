package eu.modernmt.decoder.neural.scheduler;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.TranslationTimeoutException;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.Priority;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

public class TranslationSplit {

    public final Priority priority;
    public final Sentence sentence;
    public final ScoreEntry[] suggestions;

    private Translation translation;
    private Throwable exception;

    private final long expiration;
    private int queueSize = 0;
    private long qWaitingBegin = 0;
    private long translationBegin = 0;
    private long translationEnd = 0;

    private Scheduler.TranslationLock lock;

    public TranslationSplit(Priority priority, Sentence sentence, ScoreEntry[] suggestions, long expiration) {
        this.priority = priority;
        this.sentence = sentence;
        this.suggestions = suggestions;
        this.expiration = expiration;
    }

    public boolean alignOnly() {
        return suggestions != null && suggestions.length > 0 && suggestions[0].score == 1.f;
    }

    public void setLock(Scheduler.TranslationLock lock) {
        this.lock = lock;
    }

    public void ensureValid() throws TranslationTimeoutException {
        if (expiration > 0 && expiration < System.currentTimeMillis())
            throw new TranslationTimeoutException();
    }

    public void onStartWaitingInQueue(int queueSize, long timestamp) {
        this.queueSize = queueSize;
        this.qWaitingBegin = timestamp;
    }

    public void onTranslationBegin(long timestamp) {
        this.translationBegin = timestamp;
    }

    public void onTranslationEnd(long timestamp) {
        this.translationEnd = timestamp;
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

    public int getQueueSize() {
        return queueSize;
    }

    public long getQueueWaitingBegin() {
        return qWaitingBegin;
    }

    public long getTranslationBegin() {
        return translationBegin;
    }

    public long getTranslationEnd() {
        return translationEnd;
    }

}
