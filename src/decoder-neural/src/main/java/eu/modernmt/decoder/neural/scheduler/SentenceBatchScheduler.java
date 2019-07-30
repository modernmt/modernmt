package eu.modernmt.decoder.neural.scheduler;

import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.Priority;

import java.util.*;

public class SentenceBatchScheduler extends AbstractScheduler<SentenceBatchScheduler.JobImpl> {

    public SentenceBatchScheduler(final int queueSize) {
        super(new PriorityQueue<JobImpl>(queueSize) {

            @Override
            public boolean add(JobImpl job) {
                if (offer(job))
                    return true;
                else
                    throw new IllegalStateException("Queue full");
            }

            @Override
            public boolean offer(JobImpl job) {
                if (size() + 1 > queueSize)
                    return false;

                return super.offer(job);
            }

        });
    }

    @Override
    public TranslationLock schedule(LanguageDirection direction, TranslationSplit[] splits, ScoreEntry[] suggestions) throws DecoderUnavailableException {
        CountDownTranslationLock lock = new CountDownTranslationLock(splits.length);
        for (TranslationSplit split : splits)
            split.setLock(lock);

        schedule(new JobImpl(direction, splits, suggestions));

        return lock;
    }

    @Override
    public TranslationLock schedule(LanguageDirection direction, TranslationSplit split) throws DecoderUnavailableException {
        CountDownTranslationLock lock = new CountDownTranslationLock(1);
        split.setLock(lock);

        schedule(new JobImpl(direction, split));

        return lock;
    }

    public static class JobImpl implements Scheduler.Job, Comparable<JobImpl> {

        private final LanguageDirection direction;
        private final List<TranslationSplit> splits;
        private final List<ScoreEntry> suggestions;
        private final Priority priority;
        private long timestamp;

        JobImpl(LanguageDirection direction, TranslationSplit split) {
            this(direction, Collections.singletonList(split), null);
        }

        JobImpl(LanguageDirection direction, TranslationSplit[] splits, ScoreEntry[] suggestions) {
            this(direction, Arrays.asList(splits), suggestions != null && suggestions.length > 0 ? Arrays.asList(suggestions) : null);
        }

        private JobImpl(LanguageDirection direction, List<TranslationSplit> splits, List<ScoreEntry> suggestions) {
            if (splits == null || splits.isEmpty())
                throw new IllegalArgumentException("splits cannot be null or empty");

            this.direction = direction;
            this.splits = splits;
            this.suggestions = suggestions;

            Priority priority = null;
            for (TranslationSplit split : splits) {
                if (priority == null || priority.intValue > split.priority.intValue)
                    priority = split.priority;
            }
            this.priority = priority;
        }

        @Override
        public void onStartWaitingInQueue(int queueSize) {
            this.timestamp = System.currentTimeMillis();
            for (TranslationSplit split : splits)
                split.onStartWaitingInQueue(queueSize, this.timestamp);
        }

        @Override
        public LanguageDirection getLanguageDirection() {
            return direction;
        }

        @Override
        public boolean isAlignmentJob() {
            for (TranslationSplit split : splits) {
                if (split.reference == null)
                    return false;
            }

            return true;
        }

        @Override
        public List<TranslationSplit> getTranslationSplits() {
            return splits;
        }

        @Override
        public Collection<ScoreEntry> getSuggestions() {
            return suggestions;
        }

        @Override
        public int compareTo(JobImpl o) {
            if (priority == o.priority)
                return Long.compare(timestamp, o.timestamp);
            else
                return Integer.compare(priority.intValue, o.priority.intValue);
        }
    }

}
