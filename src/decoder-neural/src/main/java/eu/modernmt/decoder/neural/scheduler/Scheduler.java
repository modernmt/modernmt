package eu.modernmt.decoder.neural.scheduler;

import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.lang.LanguageDirection;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface Scheduler extends Closeable {

    interface TranslationLock {

        void await() throws InterruptedException;

        boolean await(long timeout, TimeUnit unit) throws InterruptedException;

        void translationSplitCompleted(TranslationSplit split);

    }

    interface Job {

        LanguageDirection getLanguageDirection();

        boolean isAlignmentJob();

        List<TranslationSplit> getTranslationSplits();
    }

    /**
     * Schedule a group of translation splits (from one single translation) to be executed at some point in the future.
     *
     * @param direction         the language direction of the translation splits
     * @param translationSplits the translation splits to be translated
     * @return a {@link TranslationLock} that will unlock when all the translation splits have completed
     * @throws DecoderUnavailableException if there are too many pending translation jobs
     */
    TranslationLock schedule(LanguageDirection direction, TranslationSplit[] translationSplits) throws DecoderUnavailableException;

    /**
     * Take the next {@link Job} available for queue, waiting if necessary for one to be available.
     *
     * @return the next {@link Job} available for queue
     * @throws InterruptedException if this scheduler has been closed
     */
    Job take() throws InterruptedException;

}
