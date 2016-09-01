package eu.modernmt.processing;

import java.io.File;

/**
 * Created by davide on 31/08/16.
 */
public class ModelsPath {

    public static String ENV_VARNAME = "mmt.processing.models";

    private static File path;

    public static File get() {
        if (path == null) {
            String value = System.getProperty(ENV_VARNAME);
            if (value == null)
                throw new IllegalStateException("Missing system property '" + ENV_VARNAME + "'");

            File path = new File(value).getAbsoluteFile();
            if (!path.isDirectory())
                throw new IllegalStateException("Invalid value for system property '" + ENV_VARNAME + "'. Path is not a valid directory: " + value);

            ModelsPath.path = path;
        }

        return path;
    }

}
