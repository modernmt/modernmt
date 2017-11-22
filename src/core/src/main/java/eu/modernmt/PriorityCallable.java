package eu.modernmt;

import java.util.concurrent.Callable;

/**
 * A PriorityCallable models a Java Callable that also has a priority field.
 * Three priority values are allowed: BACKGROUND, NORMAL or HIGH.
 */
public abstract class PriorityCallable<T> implements Callable<T> {
    // the priority values are implemented by a public "Priority" enum.
    public int priority;

    public PriorityCallable() {
        this.priority = Priority.NORMAL;
    }

    public PriorityCallable(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
