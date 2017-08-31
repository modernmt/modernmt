package eu.modernmt.decoder.neural.execution;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Created by andrea on 10/08/17.
 * <p>
 * A StartNativeProcessTask is a Callable that, on execution,
 * launches and returns a new, separate NativeProcess for a decoder.
 */
public abstract class StartNativeProcessTask implements Callable<NativeProcess> {
    protected File home;
    protected File model;

    public StartNativeProcessTask(File home, File model) {
        this.home = home;
        this.model = model;
    }

    /**
     * This method launches a new decoder process.
     *
     * @return a NativeProcess object to interact with the decoder.
     */
    @Override
    public abstract NativeProcess call() throws Exception;
}
