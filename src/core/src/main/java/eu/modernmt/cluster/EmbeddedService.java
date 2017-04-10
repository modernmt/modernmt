package eu.modernmt.cluster;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 07/04/17.
 */
public abstract class EmbeddedService {

    protected List<Process> subprocesses;

    public final List<Process> getSubprocesses() {
        return Collections.unmodifiableList(subprocesses);
    }

    public abstract void shutdown();

    protected final void kill(Process process, long timeout, TimeUnit unit) {
        if (process == null || !process.isAlive())
            return;

        process.destroy();

        try {
            process.waitFor(timeout, unit);
        } catch (InterruptedException e) {
            // Nothing to do
        }

        if (process.isAlive())
            process.destroyForcibly();

        try {
            process.waitFor(timeout, unit);
        } catch (InterruptedException e) {
            // Nothing to do
        }
    }

}
