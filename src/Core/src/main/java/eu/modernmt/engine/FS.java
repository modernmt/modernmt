package eu.modernmt.engine;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 13/01/16.
 */
public class FS {

    public static final File HOME;
    public static final File ENGINES_PATH;
    public static final File RUNTIME_PATH;

    private static final String SYSPROP_MMT_HOME = "mmt.home";

    static {
        String home = System.getProperty(SYSPROP_MMT_HOME);
        if (home == null)
            throw new IllegalStateException("The system property '" + SYSPROP_MMT_HOME + "' must be initialized to the path of MMT installation.");

        HOME = new File(home);
        if (!HOME.isDirectory())
            throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + HOME + " must be a valid directory.");

        ENGINES_PATH = new File(HOME, "engines");
        if (!ENGINES_PATH.isDirectory())
            throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + ENGINES_PATH + " must be a valid directory.");

        RUNTIME_PATH = new File(HOME, "runtime");
        if (!RUNTIME_PATH.isDirectory())
            throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + RUNTIME_PATH + " must be a valid directory.");
    }

    public static File getRuntime(TranslationEngine engine, String component) throws IOException {
        return getRuntime(engine, component, true);
    }

    public static File getRuntime(TranslationEngine engine, String component, boolean ensure) throws IOException {
        File folder = new File(RUNTIME_PATH, engine.getId() + File.separatorChar + component);

        if (ensure && !folder.isDirectory())
            FileUtils.forceMkdir(folder);

        return folder;
    }
}
