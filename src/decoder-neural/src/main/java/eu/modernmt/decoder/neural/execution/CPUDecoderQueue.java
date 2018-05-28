package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.neural.NeuralDecoderException;
import eu.modernmt.decoder.neural.natv.NativeProcess;

import java.io.IOException;

class CPUDecoderQueue extends DecoderQueueImpl {

    CPUDecoderQueue(NativeProcess.Builder processBuilder, int capacity) {
        super(processBuilder, capacity);
    }

    @Override
    protected final NativeProcess startProcess(NativeProcess.Builder builder) throws NeuralDecoderException {
        try {
            return builder.startOnCPU();
        } catch (IOException e) {
            throw new NeuralDecoderException("Failed to start decoder process on CPU", e);
        }
    }

    @Override
    protected final void onProcessDied(NativeProcess process) {
        // Ignore it
    }

}
