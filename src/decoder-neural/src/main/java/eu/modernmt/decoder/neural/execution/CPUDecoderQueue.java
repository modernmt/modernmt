package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.natv.NativeProcess;

import java.io.IOException;

class CPUDecoderQueue extends DecoderQueueImpl {

    CPUDecoderQueue(NativeProcess.Builder processBuilder, int capacity) {
        super(processBuilder, capacity);
    }

    @Override
    protected final NativeProcess startProcess(NativeProcess.Builder builder) throws DecoderException {
        try {
            return builder.startOnCPU();
        } catch (IOException e) {
            throw new DecoderException("Failed to start decoder process on CPU", e);
        }
    }

    @Override
    protected final void onProcessDied(NativeProcess process) {
        // Ignore it
    }

}
