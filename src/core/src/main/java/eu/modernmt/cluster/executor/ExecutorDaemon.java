package eu.modernmt.cluster.executor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import eu.modernmt.cluster.ClusterNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 20/04/16.
 */
public class ExecutorDaemon {

    private final BlockingQueue<Task> taskQueue;
    private final Worker[] workers;
    private final Thread killer = new Thread() {
        @Override
        public void run() {
            for (Worker worker : ExecutorDaemon.this.workers)
                worker.interrupt();

            for (Worker worker : ExecutorDaemon.this.workers)
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    // Nothing to do
                }
        }
    };

    public ExecutorDaemon(HazelcastInstance hazelcast, ClusterNode node, String executorName, int capacity) {
        String taskQueueName = DistributedExecutor.TASK_QUEUE_NAME_PREFIX + executorName;
        this.taskQueue = hazelcast.getQueue(taskQueueName);
        this.workers = new Worker[capacity];

        for (int i = 0; i < capacity; i++) {
            this.workers[i] = new Worker(hazelcast, node, this.taskQueue);
            this.workers[i].start();
        }
    }

    public void shutdown() {
        killer.start();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        unit.timedJoin(killer, timeout);
        return !killer.isAlive();
    }

    private static class Worker extends Thread {

        private final Logger logger = LogManager.getLogger(Worker.class);

        private final BlockingQueue<Task> taskQueue;
        private final HazelcastInstance hazelcast;
        private final ClusterNode localNode;

        public Worker(HazelcastInstance hazelcast, ClusterNode localNode, BlockingQueue<Task> taskQueue) {
            this.hazelcast = hazelcast;
            this.taskQueue = taskQueue;
            this.localNode = localNode;
        }

        private Task next() {
            try {
                long begin = 0L;

                if (logger.isTraceEnabled()) {
                    logger.trace("Waiting for task");
                    begin = System.currentTimeMillis();
                }

                Task task = taskQueue.take();

                if (logger.isTraceEnabled()) {
                    long elapsed = System.currentTimeMillis() - begin;
                    logger.trace(String.format("Task received, waited for %.2fs", ((double) elapsed) / 1000.));
                }

                return task;
            } catch (InterruptedException e) {
                return null;
            }
        }

        @Override
        public void run() {
            Task<?> task;

            while (!isInterrupted() && (task = next()) != null) {
                TaskOutcome outcome;

                long begin = 0L;

                if (logger.isTraceEnabled()) {
                    logger.trace("Executing task " + task.resultId);
                    begin = System.currentTimeMillis();
                }

                try {
                    if (task.callable instanceof DistributedCallable)
                        ((DistributedCallable) task.callable).setLocalNode(localNode);

                    outcome = new TaskOutcome(task.resultId, task.callable.call());
                } catch (Throwable e) {
                    outcome = new TaskOutcome(e, task.resultId);
                }

                if (logger.isTraceEnabled()) {
                    long elapsed = System.currentTimeMillis() - begin;
                    logger.trace(String.format("Task completed in %.2fs", ((double) elapsed) / 1000.));
                    begin = System.currentTimeMillis();
                }

                ITopic<TaskOutcome> topic = hazelcast.getTopic(task.resultTopicId);
                topic.publish(outcome);

                if (logger.isTraceEnabled()) {
                    long elapsed = System.currentTimeMillis() - begin;
                    logger.trace(String.format("Task notified in %.2fs", ((double) elapsed) / 1000.));
                }
            }
        }
    }

}
