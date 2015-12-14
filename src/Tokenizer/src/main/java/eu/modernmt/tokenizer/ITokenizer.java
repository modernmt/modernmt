package eu.modernmt.tokenizer;

import java.io.File;

/**
 * Created by davide on 11/11/15.
 */
public abstract class ITokenizer {

    private static final String SYSPROP_MODELS_PATH = "mmt.tokenizer.models.path";
    public static final File MODELS_PATH;

    static {
        String path = System.getProperty(SYSPROP_MODELS_PATH);
        if (path == null)
            throw new IllegalStateException("The system property '" + SYSPROP_MODELS_PATH + "' must be initialized to the path of the tokenizer models folder.");

        MODELS_PATH = new File(path);
        if (!MODELS_PATH.isDirectory())
            throw new IllegalStateException("Invalid path for property '" + SYSPROP_MODELS_PATH + "': " + MODELS_PATH + " must be a valid directory.");
    }

    public abstract String[] tokenize(String text);

}
