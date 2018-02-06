package eu.modernmt.processing.chineseConverter;
/**
 * Created by nicolabertoldi on 04/02/18.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Detector holds the vocabularies of Simplified/Traditional Chinese and variants for detecting the actual language of a text
 */
public class Detector {

    protected String name;
    protected String config;
    protected Map<String,Set<String>> dicts;
    protected Map<String,Float> priorities;
    protected Map<String,String> names;
    protected String resourcesPath;

    protected boolean verbose = false;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getResourcesPath() {
        return resourcesPath;
    }

    public void setResources(String resources) {
        this.resourcesPath = resources;
    }

    public Detector() {
        this("detector");
    }

    /**
     * @param config the config to use
     */
    public Detector(String config) {
        resourcesPath = Preprocessor.class.getPackage().getName().replace('.', '/');

        dicts = new HashMap<>();
        priorities = new HashMap<>();
        names = new HashMap<>();
        this.config = "";

        setConfig(config);
        if (verbose){
            System.err.println("Detector configuration file:" + this.config);
            System.err.println("resources:" + getResourcesPath());
        }
    }


    /**
     * set config
     *
     * @param config the config to use, including "hk2s", "s2hk", "s2t", "s2tw", "s2twp",
     *               "t2hk", "t2s", "t2tw", "tw2s", and "tw2sp"
     */
    public void setConfig(String config) {
        config = config.toLowerCase();

        if (this.config.equals(config)) {
            return;
        }
        this.config = config;
        loadDict();
    }


    /**
     * load dictionary files into dictChain
     */
    private void loadDict() {
        dicts.clear();
        JsonParser jsonParser = new JsonParser();

        try {
            String filename = getResourcesPath() + "/config/" + config + ".json";
            File file = new File(filename);

            Object object = jsonParser.parse(new FileReader(file));

            JsonObject dictRoot = (JsonObject) object;
            name = dictRoot.get("name").getAsString();
            JsonArray jsonArray = dictRoot.get("dicts").getAsJsonArray();

            Float prior = 0.1f;
            for (Object obj : jsonArray) {

                JsonObject _dictRoot = (JsonObject) obj;
                String _name = _dictRoot.get("name").getAsString();
                String _description = _dictRoot.get("description").getAsString();

                Set<String> _set = new HashSet<>();

                //read all characters from the first tab-separated column of all files "dicts"
                for (JsonElement _obj : _dictRoot.get("dicts").getAsJsonArray()) {
                    File _file = new File(getResourcesPath() + "/dictionary/" + _obj.getAsString());

                    List<String> lines = Files.readAllLines(_file.toPath());
                    for (String line : lines) {
                        String[] chars = line.trim().split("\t")[0].trim().split("");
                        _set.addAll(Arrays.asList(chars));
                    }
                }
                //read all characters from the second tab-separated column of all files "revdicts"
                for (JsonElement _obj : _dictRoot.get("revdicts").getAsJsonArray()) {
                    File _file = new File(getResourcesPath() + "/dictionary/" + _obj.getAsString());

                    List<String> lines = Files.readAllLines(_file.toPath());
                    for (String line : lines) {
                        String[] chars = line.trim().split("\t")[1].trim().split("");
                        _set.addAll(Arrays.asList(chars));
                    }
                }

                //handle variants for Taiwan and Hong Kong standards
                if (_dictRoot.has("variants")) {
                    for (JsonElement _obj : _dictRoot.get("variants").getAsJsonArray()) {
                        File _file = new File(getResourcesPath() + "/dictionary/" + _obj.getAsString());

                        List<String> lines = Files.readAllLines(_file.toPath());
                        for (String line : lines) {
                            String[] from_chars = line.trim().split("\t")[0].trim().split("");
                            String[] to_chars = line.trim().split("\t")[1].trim().split("");
                            for (int i = 0; i < from_chars.length; ++i) {
                                _set.remove(from_chars[i]);
                                _set.add(to_chars[i]);
                            }
                        }
                    }
                }
                dicts.put(_name,_set);
                priorities.put(_name, prior);
                names.put(_name, _description);
                prior = prior/2;
            }

        } catch (IOException | JsonParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void printStats () {
        for (String _name : dicts.keySet()) {
            System.out.println(getDescription(_name) + ": " + dicts.get(_name).size() + " symbols");
        }
    }

    private Map<String,Float> computeScores(String string) {
        String[] chars = string.trim().split("");
        Map<String,Float> scores = new HashMap<>();

        for (String _name : dicts.keySet()) {
            scores.put(_name, priorities.get(_name) );
            scores.put(_name, 0.0f);
            for (String c : chars) {
                if (dicts.get(_name).contains(c)) {
                    scores.replace(_name, scores.get(_name), scores.get(_name) + 1);
                }
            }
        }
        return scores;
    }

    /**
     * detect the language of the string
     * @param string input string to convert
     * @return language
     */
    public String detect(String string) {
        Map<String, Float> scores = computeScores(string);

        String language = "";
        Float best = -1000.1f;
        for (String _name : scores.keySet()) {
            Float s = scores.get(_name) + priorities.get(_name);
            if (s > best) {
                best = s;
                language = _name;
            }
            if (verbose){
                System.err.println(names.get(_name) + ":" + scores.get(_name) + " out of " + string.length() + " priorities:" + priorities.get(_name));
            }
        }
        return language;
    }

    public String detectDescription(String string){
        String language = detect(string);
        return names.get(language);
    }

    public String getDescription(String language){
        return names.get(language);
    }
}
