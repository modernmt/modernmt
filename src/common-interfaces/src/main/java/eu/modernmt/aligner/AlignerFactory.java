package eu.modernmt.aligner;

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 09/05/16.
 */
public abstract class AlignerFactory {

    private static Class<? extends AlignerFactory> impl;

    public static void registerFactory(Class<? extends AlignerFactory> impl) {
        AlignerFactory.impl = impl;
    }

    public static AlignerFactory getInstance() {
        if (impl == null)
            throw new IllegalStateException("No implementation of AlignerFactory found");

        try {
            return impl.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new Error("Unable to create new instance of class " + impl.getCanonicalName(), e);
        }
    }

    protected File enginePath;

    public void setEnginePath(File enginePath) {
        this.enginePath = enginePath;
    }

    public abstract Aligner create() throws IOException;

}
