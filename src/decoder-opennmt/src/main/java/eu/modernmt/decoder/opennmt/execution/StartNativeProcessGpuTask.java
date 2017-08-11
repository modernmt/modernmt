package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by andrea on 10/08/17.
 * <p>
 * A StartNativeProcessCpuTask is a Callable that, on execution,
 * launches and returns a new NativeProcess that works on a GPU
 */
public class StartNativeProcessGpuTask extends StartNativeProcessTask {
    private int gpu;

    public StartNativeProcessGpuTask(File home, File model, int gpu) {
        super(home, model);
        this.gpu = gpu;
    }

    @Override
    public NativeProcess call() throws IOException, OpenNMTException {
        NativeProcess.Builder builder = new NativeProcess.Builder(home, model);
        NativeProcess process = null;
        /*try to launch the process; if it raises any exceptions, stop it*/
        try {
            process = builder.startOnGPU(this.gpu);
            return process;
        } catch (IOException e) {
            IOUtils.closeQuietly(process);
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        }
    }
}