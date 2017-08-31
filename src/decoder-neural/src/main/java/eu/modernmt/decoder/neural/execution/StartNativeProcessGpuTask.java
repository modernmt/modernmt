package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.neural.NeuralDecoderException;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by andrea on 10/08/17.
 * <p>
 * A StartNativeProcessGpuTask is a Callable that, on execution,
 * launches and returns a new NativeProcess that works on a GPU
 */
public class StartNativeProcessGpuTask extends StartNativeProcessTask {
    private int gpu;

    public StartNativeProcessGpuTask(File home, File model, int gpu) {
        super(home, model);
        this.gpu = gpu;
    }

    /**
     * This method launches a new decoder process to run on a GPU.
     *
     * @return a NativeProcess object to interact with the decoder.
     */
    @Override
    public NativeProcess call() throws IOException, NeuralDecoderException {
        NativeProcess.Builder builder = new NativeProcess.Builder(home, model);
        NativeProcess process = null;
        /*try to launch the process; if it raises any exceptions, stop it*/
        try {
            process = builder.startOnGPU(this.gpu);
            return process;
        } catch (IOException e) {
            IOUtils.closeQuietly(process);
            throw new NeuralDecoderException("Unable to start NMT process", e);
        }
    }
}