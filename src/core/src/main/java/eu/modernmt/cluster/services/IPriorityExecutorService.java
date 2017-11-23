package eu.modernmt.cluster.services;

import com.hazelcast.core.IExecutorService;

/**
 * An IPriorityExecutorService is an IExecutorService that accepts and handles tasks with priority.
 */
public interface IPriorityExecutorService extends IExecutorService {
    public enum Priority {
        BACKGROUND(1), LOW(2), NORMAL(3), HIGH(4), URGENT(5);

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        //my implementation of valueOf, that can not be overrided
        public static Priority getPriority(String name) {
            for(Priority priority : Priority.values())
                if(priority.name().equals(name))
                    return priority;

            //if the passed priority name does not correspond to any existing enum, throw exception
            throw new IllegalArgumentException("Invalid priority: " + name);
        }
    }

}
