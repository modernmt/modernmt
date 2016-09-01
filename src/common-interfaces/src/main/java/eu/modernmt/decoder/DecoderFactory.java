package eu.modernmt.decoder;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by davide on 09/05/16.
 */
public abstract class DecoderFactory {

    private static Class<? extends DecoderFactory> impl;

    public static void registerFactory(Class<? extends DecoderFactory> impl) {
        DecoderFactory.impl = impl;
    }

    public static DecoderFactory getInstance() {
        if (impl == null)
            throw new IllegalStateException("No implementation of DecoderFactory found");

        try {
            return impl.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new Error("Unable to create new instance of class " + impl.getCanonicalName(), e);
        }
    }

    protected File enginePath;
    protected File runtimePath;
    protected int decoderThreads;
    protected Map<String, float[]> featureWeights;

    public void setEnginePath(File enginePath) {
        this.enginePath = enginePath;
    }

    public void setRuntimePath(File runtimePath) {
        this.runtimePath = runtimePath;
    }

    public void setFeatureWeights(Map<String, float[]> featureWeights) {
        this.featureWeights = featureWeights;
    }

    public void setDecoderThreads(int decoderThreads) {
        this.decoderThreads = decoderThreads;
    }

    public abstract Decoder create() throws IOException;

}
