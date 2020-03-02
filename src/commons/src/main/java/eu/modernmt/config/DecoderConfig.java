package eu.modernmt.config;

import eu.modernmt.hw.Graphics;
import eu.modernmt.io.RuntimeIOException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * Created by davide on 04/01/17.
 */
public class DecoderConfig {

    private static final int[] DEFAULT_GPUS = new int[0];
    private static final int DEFAULT_THREADS = getDefaultThreads();

    private static int getDefaultThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        return cores > 1 ? (cores * 2) / 3 : cores;
    }

    private final EngineConfig parent;
    private int queueSize = 400;
    private int threads = DEFAULT_THREADS;
    private int[] gpus = DEFAULT_GPUS;
    private String decoderClass = null;
    private boolean enabled = true;

    public DecoderConfig(EngineConfig parent) {
        this.parent = parent;
    }

    public EngineConfig getParentConfig() {
        return parent;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

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

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
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
        if (gpus == null || gpus.length == 0) {
            this.gpus = null;
        } else {
            int[] availableGPUs;
            try {
                availableGPUs = Graphics.getAvailableGPUs();
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }

            for (int gpu : gpus) {
                if (gpu < 0 || gpu >= availableGPUs.length)
                    throw new IllegalArgumentException("Invalid GPU index: " + gpu);
            }

            this.gpus = gpus;
        }
    }

    public int getParallelismDegree() {
        return isUsingGPUs() ? gpus.length : threads;
    }

    public boolean isUsingGPUs() {
        int[] gpus = this.getGPUs();
        return (gpus != null && gpus.length != 0);
    }

    @Override
    public String toString() {
        return "Decoder: " +
                "queue=" + queueSize +
                ", threads=" + threads +
                ", gpus=" + StringUtils.join(gpus, ',') +
                ", class='" + decoderClass + '\'' +
                ", enabled=" + enabled;
    }
}
