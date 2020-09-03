package eu.modernmt.decoder.neural.scheduler;

import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;

import java.io.Closeable;
import java.util.Collection;
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

        Collection<ScoreEntry> getSuggestions();

        void onStartWaitingInQueue(int queueSize);
    }

    /**
     * Schedule a group of translation splits (from one single translation) to be translated at some point in the future.
     *
     * @param direction    the language direction of the translation splits
     * @param splits       the translation splits to be translated
     * @param suggestions  the suggestions to use to tune the engine
     * @return a {@link TranslationLock} that will unlock when all the translation splits have completed
     * @throws DecoderUnavailableException if there are too many pending translation jobs or the Scheduler has been closed
     */
    TranslationLock schedule(LanguageDirection direction, TranslationSplit[] splits, ScoreEntry[] suggestions) throws DecoderUnavailableException;

    /**
     * Schedule a a single translation split to be aligned with the given translation at some point in the future.
     *
     * @param direction   the language direction of the translation split
     * @param split       the translation split to be aligned
     * @return a {@link TranslationLock} that will unlock when alignment is completed
     * @throws DecoderUnavailableException if there are too many pending translation jobs or the Scheduler has been closed
     */
    TranslationLock schedule(LanguageDirection direction, TranslationSplit split) throws DecoderUnavailableException;

    /**
     * Take the next {@link Job} available for queue, waiting if necessary for one to be available.
     *
     * @return the next {@link Job} available for queue
     * @throws InterruptedException if this scheduler has been closed
     */
    Job take() throws InterruptedException;

}
