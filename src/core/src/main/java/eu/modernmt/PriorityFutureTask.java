package eu.modernmt;

import java.util.concurrent.*;

/**
 * A PriorityFutureTask models a Java FutureTask that also has a priority field.
 */
public class PriorityFutureTask<T> extends FutureTask<T> {

    /*We need to use FutureTask (and thus implement PriorityFutureTask) instead of just Callables because,
      we use a PriorityBlockingQueue together with our executor, and in this case only Runnables are allowed in queue.

      It is possible to pass a Callable to executorService, and it will automatically call its "newTaskFor"
      to create a new Runnable for that Callable.
      To do so, though, it is necessary to create a new class PriorityThreadPoolExecutor and the PriorityFutureTask,
      and to override newTaskFor to allow it to generate PriorityFutureTask from Callable.
     */

    private int priority;

    public PriorityFutureTask(Callable<T> callable, int priority) {
        super(callable);
        this.priority = priority;
    }

    public PriorityFutureTask(Callable<T> callable) {
        super(callable);
        this.priority = Priority.NORMAL;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
