package eu.modernmt.io;

import java.io.File;

/**
 * Created by davide on 09/05/16.
 */
public class Paths {

    public static String join(String... path) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < path.length; i++) {
            result.append(path[i]);
            if (i < path.length - 1)
                result.append(File.separatorChar);
        }

        return result.toString();
    }

    public static File join(File root, String... path) {
        return new File(root, join(path));
    }

}
