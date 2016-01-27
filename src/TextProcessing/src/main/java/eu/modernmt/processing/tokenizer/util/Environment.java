package eu.modernmt.processing.tokenizer.util;

import java.io.File;

/**
 * Created by davide on 27/01/16.
 */
public class Environment {

    private static final String SYSPROP_MMT_HOME = "mmt.home";
    public static final File MODELS_PATH;

    static {
        String home = System.getProperty(SYSPROP_MMT_HOME);
        if (home == null)
            throw new IllegalStateException("The system property '" + SYSPROP_MMT_HOME + "' must be initialized to the path of MMT installation.");

        MODELS_PATH = new File(home, "opt" + File.separatorChar + "tokenizer" + File.separatorChar + "models");
        if (!MODELS_PATH.isDirectory())
            throw new IllegalStateException("Invalid path for property '" + SYSPROP_MMT_HOME + "': " + MODELS_PATH + " must be a valid directory.");
    }

}
