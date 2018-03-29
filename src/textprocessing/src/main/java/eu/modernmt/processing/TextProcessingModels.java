package eu.modernmt.processing;

import eu.modernmt.io.FileConst;

import java.io.File;

/**
 * Created by davide on 31/08/16.
 */
public class TextProcessingModels {

    public static String ENV_VARNAME = "mmt.processing.models";

    private static File path;

    public static void setPath(File path) {
        if (path == null)
            throw new NullPointerException();

        if (!path.isDirectory())
            throw new IllegalStateException("Invalid text-processing models path: " + path);

        TextProcessingModels.path = path;
    }

    public static File getPath() {
        if (path == null) {
            String value = System.getProperty(ENV_VARNAME);

            if (value != null) {
                File path = new File(value).getAbsoluteFile();
                if (!path.isDirectory())
                    throw new IllegalStateException("Invalid value for system property '" + ENV_VARNAME + "'. Path is not a valid directory: " + value);

                TextProcessingModels.path = path;
            } else {
                TextProcessingModels.path = FileConst.getResourcePath();
            }
        }

        return path;
    }

}
