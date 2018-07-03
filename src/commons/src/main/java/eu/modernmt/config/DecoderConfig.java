package eu.modernmt.config;

import eu.modernmt.hw.Graphics;
import eu.modernmt.io.RuntimeIOException;

import java.io.IOException;

/**
 * Created by davide on 04/01/17.
 */
public class DecoderConfig {

    private static final int[] DEFAULT_GPUS = new int[0];

    private int[] gpus = DEFAULT_GPUS;
    private String decoderClass = null;
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDecoderClass() {
        return decoderClass;
    }

    public void setDecoderClass(String decoderClass) {
        this.decoderClass = decoderClass;
    }

    public int getParallelismDegree() {
        return getGPUs().length;
    }

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

        if (gpus == null || gpus.length == 0) {
            throw new IllegalArgumentException("Empty GPU list specified");
        } else {
            for (int gpu : gpus) {
                if (gpu < 0 || gpu >= availableGPUs.length)
                    throw new IllegalArgumentException("Invalid GPU index: " + gpu);
            }

            this.gpus = gpus;
        }
    }

}
