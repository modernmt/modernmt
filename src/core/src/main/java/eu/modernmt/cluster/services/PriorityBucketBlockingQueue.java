package eu.modernmt.cluster.services;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by davide on 23/11/17.
 * A PriorityBucketBlockingQueue is a PriorityBlockingQueue
 * implemented with a separate sub-queue (or bucket) for each priority.
 * <p>
 * A PriorityBucketBlockingQueue allows to define separate queue size for each subqueue.
 */
public class PriorityBucketBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    /**
     * A SubQueue is a bucket for a specific priority value in a PriorityBucketBlockingQueue.
     */
    private static class SubQueue {
        /**
         * the items that this queue contains.
         * Since the subqueue size is fixed, this is implemented as an Object array.
         */
        final Object[] items;

        /**
         * index of the first item in the list.
         * It can be used for next take, poll, peek or remove
         */
        int takeIndex;

        /**
         * index for next put, offer, or add
         */
        int putIndex;

        /**
         * Number of elements in the queue
         */
        int count;

        SubQueue(int capacity) {
            items = new Object[capacity];
        }

    }

    /**
     * The buckets for this queue.
     */
    private final SubQueue queues[];

    /**
     * Main lock guarding all access
     */
    private final ReentrantLock lock;

    /**
     * Condition for waiting takes
     */
    private final Condition notEmpty;

    /**
     * Condition for waiting puts
     */
    private final Condition notFull;


    public PriorityBucketBlockingQueue(int... capacities) {
        this(false, capacities);
    }

    public PriorityBucketBlockingQueue(boolean fair, int... capacities) {
        if (capacities == null || capacities.length == 0)
            throw new IllegalArgumentException();
        for (int c : capacities) {
            if (c <= 0)
                throw new IllegalArgumentException();
        }

        this.queues = new SubQueue[capacities.length];
        for (int i = 0; i < this.queues.length; i++)
            this.queues[i] = new SubQueue(capacities[i]);

        this.lock = new ReentrantLock(fair);
        this.notEmpty = this.lock.newCondition();
        this.notFull = this.lock.newCondition();
    }

//    /**
//     * Shared state for currently active iterators, or null if there
//     * are known not to be any.  Allows queue operations to update
//     * iterator state.
//     */
//    transient Itrs itrs = null;

    // Internal helper methods

    /**
     * Throws NullPointerException if argument is null.
     *
     * @param v the element
     */
    private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException();
    }

    private static void checkPriority(int i, int levels) {
        if (i < 0 || i >= levels)
            throw new ArrayIndexOutOfBoundsException(i);
    }

    /**
     * Inserts element at current put position in a specific priority subqueue, advances, and signals.
     * Call only when holding lock.
     */
    private void enqueue(final int priority, E x) {
        SubQueue queue = queues[priority];
        final Object[] items = queue.items;
        items[queue.putIndex] = x;
        if (++queue.putIndex == items.length)
            queue.putIndex = 0;
        queue.count++;
        notEmpty.signal();
    }

    /**
     * Extracts element at current take position, advances, and signals.
     * Call only when holding lock.
     */
    private E dequeue() {
        //for each subqueue starting from the highest priority
        for (int i = 0; i < queues.length; i++) {
            SubQueue queue = queues[i];
            if (queue.count == 0)
                continue;

            //get items and
            final Object[] items = queue.items;
            @SuppressWarnings("unchecked")

            //extract last element and replace with null
                    E x = (E) items[queue.takeIndex];

            items[queue.takeIndex] = null;
            if (++queue.takeIndex == items.length)
                queue.takeIndex = 0;
            queue.count--;
//            if (itrs != null)
//                itrs.elementDequeued(i);
            notFull.signal();
            return x;
        }

        return null;
    }

    /**
     * Deletes item at array index removeIndex.
     * Utility for remove(Object) and iterator.remove.
     * Call only when holding lock.
     */
    void removeAt(final int priority, final int removeIndex) {
        SubQueue queue = queues[priority];
        final Object[] items = queue.items;
        if (removeIndex == queue.takeIndex) {
            // removing front item; just advance
            items[queue.takeIndex] = null;
            if (++queue.takeIndex == items.length)
                queue.takeIndex = 0;
            queue.count--;
//            if (itrs != null)
//                itrs.elementDequeued(priority);
        } else {
            // an "interior" remove

            // slide over all others up through putIndex.
            final int putIndex = queue.putIndex;
            for (int i = removeIndex; ; ) {
                int next = i + 1;
                if (next == items.length)
                    next = 0;
                if (next != putIndex) {
                    items[i] = items[next];
                    i = next;
                } else {
                    items[i] = null;
                    queue.putIndex = i;
                    break;
                }
            }
            queue.count--;
//            if (itrs != null)
//                itrs.removedAt(priority, removeIndex);
        }
        notFull.signal();
    }

    private int totalCount() {
        int count = 0;
        for (SubQueue queue : queues) count += queue.count;
        return count;
    }

    private static int getPriority(Object e) {
        if (e instanceof Prioritizable)
            return ((Prioritizable) e).getPriority();
        else
            return 0;
    }

    public boolean add(E e) {
        return super.add(e);
    }

    public boolean offer(E e) {
        checkNotNull(e);

        final int priority = getPriority(e);
        checkPriority(priority, queues.length);

        final SubQueue queue = queues[priority];
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (queue.count == queue.items.length)
                return false;
            else {
                enqueue(priority, e);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    public void put(E e) throws InterruptedException {
        checkNotNull(e);
        final int priority = getPriority(e);
        checkPriority(priority, queues.length);

        final SubQueue queue = queues[priority];
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (queue.count == queue.items.length)
                notFull.await();
            enqueue(priority, e);
        } finally {
            lock.unlock();
        }
    }

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        checkNotNull(e);
        final int priority = getPriority(e);
        checkPriority(priority, queues.length);

        final SubQueue queue = queues[priority];
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (queue.count == queue.items.length) {
                if (nanos <= 0)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(priority, e);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (totalCount() == 0)
                notEmpty.await();

            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (totalCount() == 0) {
                if (nanos <= 0)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (SubQueue queue : queues) {
                @SuppressWarnings("unchecked")
                E e = (E) queue.items[queue.takeIndex];
                if (e != null)
                    return e;
            }

            return null;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return totalCount();
        } finally {
            lock.unlock();
        }
    }

    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int capacity = 0;
            for (SubQueue queue : queues) capacity += (queue.items.length - queue.count);
            return capacity;
        } finally {
            lock.unlock();
        }
    }

    public boolean remove(Object o) {
        if (o == null) return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (int p = 0; p < queues.length; p++) {
                SubQueue queue = queues[p];
                final Object[] items = queue.items;

                if (queue.count > 0) {
                    final int putIndex = queue.putIndex;
                    int i = queue.takeIndex;
                    do {
                        if (o.equals(items[i])) {
                            removeAt(p, i);
                            return true;
                        }
                        if (++i == items.length)
                            i = 0;
                    } while (i != putIndex);
                }
            }

            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(Object o) {
        if (o == null) return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (int p = 0; p < queues.length; p++) {
                SubQueue queue = queues[p];
                final Object[] items = queue.items;

                if (queue.count > 0) {
                    final int putIndex = queue.putIndex;
                    int i = queue.takeIndex;
                    do {
                        if (o.equals(items[i]))
                            return true;
                        if (++i == items.length)
                            i = 0;
                    } while (i != putIndex);
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public Object[] toArray() {
        Object[] a;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            a = new Object[totalCount()];

            int dest = 0;

            for (SubQueue queue : queues) {
                int n = queue.items.length - queue.takeIndex;
                if (queue.count <= n)
                    System.arraycopy(queue.items, queue.takeIndex, a, dest, queue.count);
                else {
                    System.arraycopy(queue.items, queue.takeIndex, a, dest, n);
                    System.arraycopy(queue.items, 0, a, n, queue.count - n);
                }

                dest += queue.count;
            }
        } finally {
            lock.unlock();
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            final int totalCount = totalCount();
            final int len = a.length;
            if (len < totalCount)
                a = (T[]) java.lang.reflect.Array.newInstance(
                        a.getClass().getComponentType(), totalCount);

            int dest = 0;

            for (SubQueue queue : queues) {
                final Object[] items = queue.items;
                int n = items.length - queue.takeIndex;
                if (queue.count <= n)
                    System.arraycopy(items, queue.takeIndex, a, dest, queue.count);
                else {
                    System.arraycopy(items, queue.takeIndex, a, dest, n);
                    System.arraycopy(items, 0, a, n, queue.count - n);
                }

                dest += queue.count;
            }

            if (len > totalCount)
                a[totalCount] = null;
        } finally {
            lock.unlock();
        }
        return a;
    }

    @Override
    public String toString() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (totalCount() == 0)
                return "[]";

            StringBuilder sb = new StringBuilder();
            sb.append('[');

            for (SubQueue queue : queues) {
                sb.append('#');
                int k = queue.count;
                final Object[] items = queue.items;

                for (int i = queue.takeIndex; ; ) {
                    Object e = items[i];
                    sb.append(e == this ? "(this Collection)" : e);
                    if (--k == 0)
                        break;
                    sb.append(',').append(' ');
                    if (++i == items.length)
                        i = 0;
                }
            }

            return sb.append(']').toString();
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int totalCount = totalCount();
            if (totalCount > 0) {
                doClear();

//                if (itrs != null)
//                    itrs.queueIsEmpty();
                for (; totalCount > 0 && lock.hasWaiters(notFull); totalCount--)
                    notFull.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    private void doClear() {
        for (SubQueue queue : queues) {
            if (queue.count == 0)
                continue;

            final Object[] items = queue.items;

            final int putIndex = queue.putIndex;
            int i = queue.takeIndex;
            do {
                items[i] = null;
                if (++i == items.length)
                    i = 0;
            } while (i != putIndex);
            queue.takeIndex = putIndex;
            queue.count = 0;
        }
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        throw new UnsupportedOperationException();
    }

    public int drainTo(Collection<? super E> c) {
        checkNotNull(c);
        if (c == this)
            throw new IllegalArgumentException();

        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int totalCount = totalCount();
            if (totalCount > 0) {
                for (SubQueue queue : queues) {
                    int n = queue.items.length - queue.takeIndex;
                    if (queue.count <= n) {
                        for (int i = 0; i < queue.count; i++) {
                            @SuppressWarnings("unchecked")
                            E x = (E) queue.items[queue.takeIndex + i];
                            c.add(x);
                        }
                    } else {
                        for (int i = 0; i < n; i++) {
                            @SuppressWarnings("unchecked")
                            E x = (E) queue.items[queue.takeIndex + i];
                            c.add(x);
                        }

                        for (int i = 0; i < queue.count - n; i++) {
                            @SuppressWarnings("unchecked")
                            E x = (E) queue.items[i];
                            c.add(x);
                        }
                    }
                }

                doClear();

//                if (itrs != null) {
//                    if (count == 0)
//                        itrs.queueIsEmpty();
//                    else if (i > take)
//                        itrs.takeIndexWrapped();
//                }

                for (; totalCount > 0 && lock.hasWaiters(notFull); totalCount--)
                    notFull.signal();
            }

            return totalCount;
        } finally {
            lock.unlock();
        }
    }

    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

}
