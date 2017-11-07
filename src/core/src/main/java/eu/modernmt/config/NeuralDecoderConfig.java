package eu.modernmt.config;

import eu.modernmt.hw.Graphics;

import java.util.Arrays;

/**
 * Created by davide on 06/07/17.
 */
public class NeuralDecoderConfig extends DecoderConfig {

    private int[] gpus = Graphics.getAvailableGPUs();

    public int[] getGPUs() {
        return gpus;
    }

    // exclude gpus, specified in the config file, but not available
    // multiple occurrences of the same gpu are allowed
    public void setGPUs(int[] gpus) {
        int[] availableGPUs = Graphics.getAvailableGPUs();

        if (availableGPUs.length == 0 || gpus == null || gpus.length == 0) {
            this.gpus = null;
        } else {
            int size = 0;
            for (int i = 0; i < gpus.length; i++) {
                if (0 <= gpus[i] && gpus[i] < availableGPUs.length)
                    size++;
                else
                    gpus[i] = -1;
            }

            int[] copy = null;

            if (size > 0) {
                copy = new int[size];

                int i = 0;
                for (int gpu : gpus) {
                    if (gpu >= -1)
                        copy[i++] = gpu;
                }
            }

            this.gpus = copy;
        }
    }

    @Override
    public int getParallelismDegree() {
        return gpus == null || gpus.length == 0 ? Runtime.getRuntime().availableProcessors() : gpus.length;
    }

    @Override
    public String toString() {
        return "[Neural decoder]\n" +
                "  Graphics = " + Arrays.toString(gpus) + "\n" +
                "  enabled = " + enabled;
    }
}
