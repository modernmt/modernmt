package eu.modernmt.decoder.neural.execution.impl;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class HandlerBlockingQueue {

    private final Handler[] items;
    private int takeIndex;
    private int putIndex;
    private int count;

    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final Condition notFull;

    public HandlerBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0)
            throw new IllegalArgumentException();

        items = new Handler[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }

    /**
     * Inserts the specified element at the tail of this queue if it is
     * possible to do so immediately without exceeding the queue's capacity,
     * returning {@code true} upon success and {@code false} if this queue
     * is full.
     *
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(Handler e) {
        Objects.requireNonNull(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == items.length)
                return false;
            else {
                enqueue(e);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @param checkpoint preferred most-recently used checkpoint
     * @return the selected instance or the head of this queue if there is no Handler with preferred checkpoint
     * @throws InterruptedException if interrupted while waiting
     */
    public Handler take(File checkpoint) throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0)
                notEmpty.await();

            Handler result = checkpoint == null ? null : select(checkpoint);
            return result == null ? dequeue() : result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    public Handler poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (count == 0) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * @param checkpoint preferred most-recently used checkpoint
     * @param timeout    how long to wait before giving up, in units of
     *                   {@code unit}
     * @param unit       a {@code TimeUnit} determining how to interpret the
     *                   {@code timeout} parameter
     * @return the selected instance or the head of this queue if there is no Handler with preferred checkpoint,
     * or {@code null} if the specified waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    public Handler poll(File checkpoint, long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                if (nanos <= 0L)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }

            Handler result = checkpoint == null ? null : select(checkpoint);
            return result == null ? dequeue() : result;
        } finally {
            lock.unlock();
        }
    }

    // Internal helper methods

    private void enqueue(Handler e) {
        final Object[] items = this.items;
        items[putIndex] = e;
        if (++putIndex == items.length) putIndex = 0;
        count++;
        notEmpty.signal();
    }

    private Handler dequeue() {
        final Handler[] items = this.items;

        Handler e = items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length) takeIndex = 0;
        count--;
        notFull.signal();
        return e;
    }

    private Handler select(File checkpoint) {
        if (count > 0) {
            final Handler[] items = this.items;
            for (int i = takeIndex, end = putIndex,
                 to = (i < end) ? end : items.length;
                    ; i = 0, to = end) {
                for (; i < to; i++)
                    if (checkpoint.equals(items[i].getLastCheckpoint())) {
                        Handler result = items[i];
                        removeAt(i);
                        return result;
                    }
                if (to == end) break;
            }
        }

        return null;
    }

    private void removeAt(final int removeIndex) {
        final Object[] items = this.items;
        if (removeIndex == takeIndex) {
            // removing front item; just advance
            items[takeIndex] = null;
            if (++takeIndex == items.length) takeIndex = 0;
            count--;
        } else {
            // an "interior" remove

            // slide over all others up through putIndex.
            for (int i = removeIndex, putIndex = this.putIndex; ; ) {
                int pred = i;
                if (++i == items.length) i = 0;
                if (i == putIndex) {
                    items[pred] = null;
                    this.putIndex = pred;
                    break;
                }
                items[pred] = items[i];
            }
            count--;
        }
        notFull.signal();
    }

}
