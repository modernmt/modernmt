package eu.modernmt.decoder.neural.scheduler;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.TranslationTimeoutException;
import eu.modernmt.model.Priority;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

public class TranslationSplit {

    public final Priority priority;
    public final Sentence sentence;
    public final String[] reference;

    private Translation translation;
    private Throwable exception;

    private final long expiration;
    private int queueSize = 0;
    private long qWaitingBegin = 0;
    private long translationBegin = 0;
    private long translationEnd = 0;

    private Scheduler.TranslationLock lock;

    public TranslationSplit(Priority priority, Sentence sentence, long expiration) {
        this(priority, sentence, null, expiration);
    }

    public TranslationSplit(Priority priority, Sentence sentence, String[] reference, long expiration) {
        this.priority = priority;
        this.sentence = sentence;
        this.reference = reference;
        this.expiration = expiration;
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

    public void setTranslation(Translation translation) {
        this.translation = translation;
        this.translationEnd = System.currentTimeMillis();

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

    public long getQueueTime() {
        return translationBegin - qWaitingBegin;
    }

    public long getTranslationTime() {
        return translationEnd - translationBegin;
    }

}
