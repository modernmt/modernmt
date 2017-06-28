package eu.modernmt;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by davide on 28/06/17.
 */
public class Pom {

    private static Properties pomProperties = null;

    public static String getProperty(String name) {
        if (pomProperties == null) {
            synchronized (Pom.class) {
                if (pomProperties == null) {
                    InputStream stream = null;

                    try {
                        stream = Pom.class.getClassLoader().getResourceAsStream("pom-root.properties");
                        Properties properties = new Properties();
                        properties.load(stream);

                        pomProperties = properties;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                }
            }
        }

        return pomProperties.getProperty(name);
    }

}
