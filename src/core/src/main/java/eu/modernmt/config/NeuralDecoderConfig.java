package eu.modernmt.config;

import eu.modernmt.hw.Graphics;

import java.util.Arrays;

/**
 * Created by davide on 06/07/17.
 */
public class NeuralDecoderConfig extends DecoderConfig {

    private static final int[] DEFAULT_GPUS = Graphics.getAvailableGPUs();

    private int[] gpus = DEFAULT_GPUS;

    public int[] getGPUs() {
        return gpus;
    }

    public void setGPUs(int[] gpus) {
        this.gpus = gpus;
    }

    @Override
    public int getParallelismDegree() {
        return gpus == null ? Runtime.getRuntime().availableProcessors() : gpus.length;
    }

    @Override
    public String toString() {
        return "[Neural decoder]\n" +
                "  Graphics = " + Arrays.toString(gpus) + "\n" +
                "  enabled = " + enabled;
    }
}
