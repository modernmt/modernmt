package eu.modernmt.context;

import java.io.File;

/**
 * Created by davide on 09/05/16.
 */
public abstract class ContextAnalyzerFactory {

    private static Class<? extends ContextAnalyzerFactory> impl;

    public static void registerFactory(Class<? extends ContextAnalyzerFactory> impl) {
        ContextAnalyzerFactory.impl = impl;
    }

    public static ContextAnalyzerFactory getInstance() {
        if (impl == null)
            throw new IllegalStateException("No implementation of ContextAnalyzerFactory found");

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

    public abstract ContextAnalyzer create() throws ContextAnalyzerException;

}
