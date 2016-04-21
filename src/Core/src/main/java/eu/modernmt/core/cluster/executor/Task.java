package eu.modernmt.core.cluster.executor;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Created by davide on 20/04/16.
 */
class Task<V> implements Serializable {

    public final Callable<V> callable;
    public final String resultTopicId;
    public final long resultId;

    public Task(Callable<V> callable, String resultQueueId, long resultId) {
        this.callable = callable;
        this.resultTopicId = resultQueueId;
        this.resultId = resultId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Task task = (Task) o;

        if (resultId != task.resultId) return false;
        return resultTopicId.equals(task.resultTopicId);

    }

    @Override
    public int hashCode() {
        int result = resultTopicId.hashCode();
        result = 31 * result + (int) (resultId ^ (resultId >>> 32));
        return result;
    }
}
