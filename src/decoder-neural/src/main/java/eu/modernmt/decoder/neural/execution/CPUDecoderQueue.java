package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.neural.natv.NativeProcess;

import java.io.IOException;

class CPUDecoderQueue extends DecoderQueueImpl {

    CPUDecoderQueue(NativeProcess.Builder processBuilder, int capacity) {
        super(processBuilder, capacity);
    }

    @Override
    protected final NativeProcess startProcess(NativeProcess.Builder builder) throws IOException {
        return builder.startOnCPU();
    }

    @Override
    protected final void onProcessDied(NativeProcess process) {
        // Ignore it
    }

}
