package eu.modernmt.cluster.executor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by davide on 20/04/16.
 */
public class DistributedExecutor extends AbstractExecutorService {

    static final String TASK_QUEUE_NAME_PREFIX = "cluster.DistributedExecutor.Queue#";
    private static final String OUTCOME_TOPIC_NAME_PREFIX = "cluster.DistributedExecutor.Topic#";
    private static final String ID_GENERATOR_NAME_PREFIX = "cluster.DistributedExecutor.IdGenerator";

    private final HashMap<Long, RemoteFutureTask<?>> pendingTasks = new HashMap<>();
    private final AtomicLong taskIdGenerator = new AtomicLong(0L);
    private final String outcomeTopicId;
    private final BlockingQueue<Task> taskQueue;
    private final ITopic<TaskOutcome> taskOutcomeTopic;

    private boolean shutdown = false;

    public DistributedExecutor(HazelcastInstance hazelcast, String name) {
        long id = hazelcast.getIdGenerator(ID_GENERATOR_NAME_PREFIX).newId();
        String taskQueueName = TASK_QUEUE_NAME_PREFIX + name;

        outcomeTopicId = OUTCOME_TOPIC_NAME_PREFIX + id;
        taskQueue = hazelcast.getQueue(taskQueueName);
        taskOutcomeTopic = hazelcast.getTopic(outcomeTopicId);
        taskOutcomeTopic.addMessageListener(this::onTaskOutcome);
    }

    @Override
    public void shutdown() {
        shutdown = true;
        taskOutcomeTopic.destroy();
    }

    @NotNull
    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    protected <T> RemoteFutureTask<T> newTaskFor(final Runnable runnable, final T value) {
        long id = taskIdGenerator.addAndGet(1L);

        Task<T> task = new Task<>(() -> {
            runnable.run();
            return value;
        }, outcomeTopicId, id);

        return new RemoteFutureTask<>(task, this);
    }

    @Override
    protected <T> RemoteFutureTask<T> newTaskFor(Callable<T> callable) {
        long id = taskIdGenerator.addAndGet(1L);
        Task<T> task = new Task<>(callable, outcomeTopicId, id);
        return new RemoteFutureTask<>(task, this);
    }

    @Override
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();

        RemoteFutureTask future = command instanceof RemoteFutureTask ? (RemoteFutureTask) command : newTaskFor(command, null);
        Task<?> task = future.getTask();

        pendingTasks.put(task.resultId, future);
        if (!taskQueue.offer(task)) {
            pendingTasks.remove(task.resultId);
            throw new RejectedExecutionException("Task cannot be added to the execution queue: queue is full.");
        }
    }

    @SuppressWarnings("unchecked")
    private void onTaskOutcome(Message<TaskOutcome> message) {
        TaskOutcome outcome = message.getMessageObject();
        RemoteFutureTask task = pendingTasks.remove(outcome.id);

        if (task != null) {
            if (outcome.exception == null)
                task.set(outcome.value);
            else
                task.setException(outcome.exception);
        }
    }

    void cancel(RemoteFutureTask task) {
        pendingTasks.remove(task.getTask().resultId);
    }

}
