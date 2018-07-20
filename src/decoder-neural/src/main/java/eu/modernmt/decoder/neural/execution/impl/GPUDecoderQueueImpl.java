package eu.modernmt.decoder.neural.execution.impl;

import eu.modernmt.decoder.neural.execution.PythonDecoder;

import java.io.IOException;

class GPUDecoderQueueImpl extends DecoderQueueImpl {

    private final int[] gpus;
    private int idx;

    protected GPUDecoderQueueImpl(PythonDecoder.Builder processBuilder, int[] gpus) {
        super(processBuilder, gpus.length);

        this.gpus = gpus;
        this.idx = this.gpus.length - 1;
    }

    @Override
    protected PythonDecoder startProcess(PythonDecoder.Builder builder) throws IOException {
        int gpu;

        synchronized (this) {
            gpu = this.gpus[this.idx--];
        }

        return builder.startOnGPU(gpu);
    }

    @Override
    protected void onProcessDied(PythonDecoder process) {
        synchronized (this) {
            this.gpus[++this.idx] = process.getGPU();
        }
    }

}
