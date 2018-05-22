package eu.modernmt.cluster.db;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.model.Memory;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrea on 28/03/17.
 * <p>
 * This class manages the database creation and initialization.
 * It is only employed when a node is performing mmt start
 * and a DB is being build locally (in the same machine).
 */
class BaselineMemoryCollection {

    /**
     * This method reads the memories json file and returns
     * the list of memories that it contained
     *
     * @return the list of parsed memories
     */
    public static List<Memory> load(File memoriesJsonPath) {
        Gson gson = new Gson();
        JsonReader jsonReader = null;
        Type MEMORIES_TYPE = new TypeToken<List<Memory>>() {
        }.getType();

        try {
            jsonReader = new JsonReader(
                    new InputStreamReader(
                            new FileInputStream(memoriesJsonPath),
                            UTF8Charset.get()
                    )
            );
            return gson.fromJson(jsonReader, MEMORIES_TYPE);
        } catch (FileNotFoundException e) {
            return new ArrayList<>(0);
        } finally {
            IOUtils.closeQuietly(jsonReader);
        }

    }
}