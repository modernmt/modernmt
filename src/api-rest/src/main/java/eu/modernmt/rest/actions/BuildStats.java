package eu.modernmt.rest.actions;

import com.google.gson.JsonObject;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.JSONObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by davide on 23/12/15.
 */
@Route(aliases = "_build", method = HttpMethod.GET)
public class BuildStats extends JSONObjectAction {

    private static Properties sysprop = null;

    public static Properties getSysProp() throws IOException {
        if (sysprop == null) {
            synchronized (BuildStats.class) {
                if (sysprop == null) {
                    InputStream stream = null;

                    try {
                        stream = BuildStats.class.getClassLoader().getResourceAsStream("pom-root.properties");
                        Properties properties = new Properties();
                        properties.load(stream);
                        sysprop = properties;
                    } finally {
                        IOUtils.closeQuietly(stream);
                    }
                }
            }
        }

        return sysprop;
    }

    @Override
    protected JsonObject execute(RESTRequest req, Parameters params) throws IOException {
        Properties sysprop = getSysProp();

        JsonObject result = new JsonObject();
        result.addProperty("version", sysprop.getProperty("version"));
        result.addProperty("number", sysprop.getProperty("build"));

        return result;
    }

}
