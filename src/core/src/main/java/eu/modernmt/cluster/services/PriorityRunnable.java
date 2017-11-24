package eu.modernmt.cluster.services;

/**
 * A PriorityRunnable is a Runnable that has a priority value
 * that must be taken into account when figuring the runnables execution order.
 */
public class PriorityRunnable implements Runnable, Prioritizable {
    private final int priority;

    public PriorityRunnable(int priority) {
        this.priority = priority;
    }

    public void run() {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            // Do nothing
        }
    }

    public int getPriority() {
        return priority;
    }
}

