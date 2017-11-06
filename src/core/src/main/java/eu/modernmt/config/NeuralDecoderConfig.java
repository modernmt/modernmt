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

//    public void setGPUs(int[] gpus) {
//        this.gpus = gpus;
//    }

    public void setGPUs(int[] gpus) {
        // exclude gpus, specified in the config file, but not available
        // multiple occurrences of the same gpu are allowed
        int gpuCount = 0;
        for (int i = 0; i < gpus.length; i++) {
            if (gpus[i] < DEFAULT_GPUS.length) {
                gpuCount++;
            } else {
                gpus[i] = -1;
            }
        }

        if (gpuCount>0) {
            this.gpus= new int[gpuCount];
            gpuCount = 0;
            for (int i = 0; i < gpus.length; i++) {
                if (gpus[i] != -1) {
                    this.gpus[gpuCount++] = gpus[i];
                }
            }
        } else{
            this.gpus = DEFAULT_GPUS;
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
