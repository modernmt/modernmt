package eu.modernmt.processing.chinese;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dictionary holds the mappings for converting Chinese characters
 */
class Dictionary {

    protected String name;
    protected String config;
    protected List<Map<String, String>> dictChain;
    protected Map<String, String> dict;
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

    /**
     * @param config the config to use, including "hk2s", "s2hk", "s2t", "s2tw", "s2twp",
     *               "t2hk", "t2s", "t2tw", "tw2s", and "tw2sp"
     */
    public Dictionary(String config) {
        resourcesPath = getClass().getPackage().getName().replace('.', '/');

        dictChain = new ArrayList<>();

        name = "";
        this.config = "";

        setConfig(config);
        if (verbose) {
            System.err.println("resources:" + getResourcesPath());
            System.err.println("Dictionary configuration file:" + this.config);
        }
    }

    /**
     * @return dict name
     */
    public String getDictName() {
        return name;
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

        dictChain.clear();

        switch (config) {
            case "hk2s":
            case "s2hk":
            case "s2t":
            case "s2tw":
            case "s2twp":
            case "t2hk":
            case "t2s":
            case "t2tw":
            case "tw2s":
            case "tw2sp":
                this.config = config;
                break;
            default:
                this.config = "s2t";
                break;
        }

        loadDict();
    }


    /**
     * load dictionary files into dictChain
     */
    private void loadDict() {
        dictChain.clear();
        JsonParser jsonParser = new JsonParser();

        List<String> dictFileNames = new ArrayList<>();

        try {
            String filename = getResourcesPath() + "/config/" + config + ".json";
            File file = new File(filename);

            Object object = jsonParser.parse(new FileReader(file));

            JsonObject dictRoot = (JsonObject) object;

            name = dictRoot.get("name").getAsString();
            JsonArray jsonArray = dictRoot.get("conversion_chain").getAsJsonArray();

            for (Object obj : jsonArray) {
                JsonObject dictObj = (JsonObject) ((JsonObject) obj).get("dict");
                dictFileNames.addAll(getDictFileNames(dictObj));
            }

        } catch (IOException | JsonParseException e) {
            e.printStackTrace();
        }

        for (String filename : dictFileNames) {
            dict = new HashMap<>();
            filename = getResourcesPath() + "/dictionary/" + filename;

            try {
                File file = new File(filename);
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    String[] words = line.trim().split("\t");
                    dict.put(words[0], words[1]);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            dictChain.add(dict);
        }

    }

    private List<String> getDictFileNames(JsonObject dictObject) {
        List<String> filenames = new ArrayList<>();

        String type = dictObject.get("type").getAsString();

        if (type.equals("txt")) {
            filenames.add(dictObject.get("file").getAsString());
        } else if (type.equals("group")) {
            JsonArray dictGroup = (JsonArray) dictObject.get("dicts");
            for (Object obj : dictGroup) {
                filenames.addAll(getDictFileNames((JsonObject) obj));
            }
        }

        return filenames;
    }


    /**
     * @return dictChain
     */
    public List<Map<String, String>> getDictChain() {
        return dictChain;
    }
}
