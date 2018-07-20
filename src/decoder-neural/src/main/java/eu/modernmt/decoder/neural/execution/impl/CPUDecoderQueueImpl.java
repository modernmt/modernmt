package eu.modernmt.decoder.neural.execution.impl;

import eu.modernmt.decoder.neural.execution.PythonDecoder;

import java.io.IOException;

class CPUDecoderQueueImpl extends DecoderQueueImpl {

    protected CPUDecoderQueueImpl(PythonDecoder.Builder processBuilder, int capacity) {
        super(processBuilder, capacity);
    }

    @Override
    protected PythonDecoder startProcess(PythonDecoder.Builder processBuilder) throws IOException {
        return processBuilder.startOnCPU();
    }

    @Override
    protected void onProcessDied(PythonDecoder process) {
        // Ignore it
    }

}
