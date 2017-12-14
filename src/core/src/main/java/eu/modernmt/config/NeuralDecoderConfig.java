package eu.modernmt.config;

import eu.modernmt.hw.Graphics;
import eu.modernmt.io.RuntimeIOException;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by davide on 06/07/17.
 */
public class NeuralDecoderConfig extends DecoderConfig {

    private static final int[] DEFAULT_GPUS = new int[0];

    private int[] gpus = DEFAULT_GPUS;

    public int[] getGPUs() {
        if (gpus == DEFAULT_GPUS) {
            try {
                gpus = Graphics.getAvailableGPUs();
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        }
        return gpus;
    }

    // exclude gpus, specified in the config file, but not available
    // multiple occurrences of the same gpu are allowed
    public void setGPUs(int[] gpus) {
        int[] availableGPUs;
        try {
            availableGPUs = Graphics.getAvailableGPUs();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }

        if (availableGPUs.length == 0 || gpus == null || gpus.length == 0) {
            this.gpus = null;
        } else {
            for (int gpu : gpus) {
                if (gpu < 0 || gpu >= availableGPUs.length)
                    throw new IllegalArgumentException("Invalid GPU index: " + gpu);
            }

            this.gpus = gpus;
        }
    }

    @Override
    public int getThreads() {
        return threads;
    }

    @Override
    public void setThreads(int threads) {
        this.threads = threads;
    }

    @Override
    public int getParallelismDegree() {
        int[] gpus = this.getGPUs();
        return (gpus != null && gpus.length != 0) ? gpus.length : threads;
    }

    public boolean isUsingGPUs() {
        int[] gpus = this.getGPUs();
        return (gpus != null && gpus.length != 0);
    }

    @Override
    public String toString() {
        return "[Neural decoder]\n" +
                "  Graphics = " + Arrays.toString(gpus) + "\n" +
                "  enabled = " + enabled;
    }
}
