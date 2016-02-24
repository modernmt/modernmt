package eu.modernmt.config;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 24/02/16.
 */
public class FileSystemConfig {

    public final File home;
    public final File engines;
    public final File runtime;
    public final File tokenizerModels;

    private static final String SYSPROP_MMT_HOME = "mmt.home";

    FileSystemConfig() {
        String home = System.getProperty(SYSPROP_MMT_HOME);
        if (home == null)
            throw new IllegalStateException("The system property '" + SYSPROP_MMT_HOME + "' must be initialized to the path of MMT installation.");

        this.home = new File(home);
        if (!this.home.isDirectory())
            throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + this.home + " must be a valid directory.");

        this.engines = new File(this.home, "engines");
        if (!this.engines.isDirectory())
            throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + this.engines + " must be a valid directory.");

        this.runtime = new File(this.home, "runtime");
        if (!this.runtime.isDirectory())
            throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + this.runtime + " must be a valid directory.");

        tokenizerModels = new File(this.home, "opt" + File.separatorChar + "tokenizer" + File.separatorChar + "models");
        if (!tokenizerModels.isDirectory())
            throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + tokenizerModels + " must be a valid directory.");
    }

    public File getRuntime(String engine, String component) throws IOException {
        return getRuntime(engine, component, true);
    }

    public File getRuntime(String engine, String component, boolean ensure) throws IOException {
        File folder = new File(runtime, engine + File.separatorChar + component);

        if (ensure && !folder.isDirectory())
            FileUtils.forceMkdir(folder);

        return folder;
    }
}
