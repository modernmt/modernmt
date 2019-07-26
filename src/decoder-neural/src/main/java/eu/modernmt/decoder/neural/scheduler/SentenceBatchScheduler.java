package eu.modernmt.decoder.neural.scheduler;

import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Priority;

import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SentenceBatchScheduler implements Scheduler {

    private final PriorityQueue<Job> queue;
    private final int maxQueueSize;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmptyCondition = lock.newCondition();
    private boolean active = true;

    public SentenceBatchScheduler(int queueSize) {
        queue = new PriorityQueue<>(queueSize);
        maxQueueSize = queueSize;
    }

    @Override
    public TranslationLock schedule(LanguageDirection direction, TranslationSplit[] translationSplits) throws DecoderUnavailableException {
        CountDownTranslationLock tLock = new CountDownTranslationLock(translationSplits.length);
        for (TranslationSplit split : translationSplits)
            split.setLock(tLock);

        try {
            lock.lock();

            int qSize = queue.size();

            if (qSize + translationSplits.length > maxQueueSize)
                throw new DecoderUnavailableException("Decoder unavailable due to a temporary overloading");
            if (!active)
                throw new DecoderUnavailableException("Decoder has been shut down");

            queue.add(new JobImpl(qSize, direction, translationSplits));
        } finally {
            notEmptyCondition.signalAll();
            lock.unlock();
        }

        return tLock;
    }

    @Override
    public Job take() throws InterruptedException {
        try {
            lock.lock();
            while (queue.isEmpty() && active)
                notEmptyCondition.await();

            if (queue.isEmpty())
                throw new InterruptedException();

            return queue.poll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        try {
            lock.lock();
            active = false;
        } finally {
            notEmptyCondition.signalAll();
            lock.unlock();
        }
    }

    private static class JobImpl implements Scheduler.Job, Comparable<JobImpl> {

        private final LanguageDirection direction;
        private final List<TranslationSplit> splits;
        private final long timestamp;
        private final Priority priority;

        JobImpl(int qSize, LanguageDirection direction, TranslationSplit[] splits) {
            this.direction = direction;
            this.splits = Arrays.asList(splits);

            this.timestamp = System.currentTimeMillis();
            for (TranslationSplit split : splits)
                split.onStartWaitingInQueue(qSize, this.timestamp);

            Priority priority = null;
            for (TranslationSplit split : splits) {
                if (priority == null || priority.intValue > split.priority.intValue)
                    priority = split.priority;
            }
            this.priority = priority;
        }

        @Override
        public LanguageDirection getLanguageDirection() {
            return direction;
        }

        @Override
        public boolean isAlignmentJob() {
            for (TranslationSplit split : splits) {
                if (!split.alignOnly())
                    return false;
            }

            return true;
        }

        @Override
        public List<TranslationSplit> getTranslationSplits() {
            return splits;
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
