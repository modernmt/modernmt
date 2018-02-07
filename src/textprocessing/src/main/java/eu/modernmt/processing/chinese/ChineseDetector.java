package eu.modernmt.processing.chinese;
/**
 * Created by nicolabertoldi on 04/02/18.
 */


import com.google.gson.*;
import eu.modernmt.lang.Language;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

/**
 * ChineseDetector holds the vocabularies of Simplified/Traditional Chinese and variants for detecting the actual language of a text
 */
public class ChineseDetector {

    protected String name;
    protected String config;
    protected Map<String, Set<String>> dicts;
    protected Map<String, Float> priorities;
    protected Map<String, String> names;

    public ChineseDetector() {
        this("detector");
    }

    public Map<String, String> getNames() {
        return names;
    }

    /**
     * @param config the config to use
     */
    public ChineseDetector(String config) {
        dicts = new HashMap<>();
        priorities = new HashMap<>();
        names = new HashMap<>();
        this.config = "";

        setConfig(config);
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
        printSupportedLanguages();
    }


    /**
     * load dictionary files into dictChain
     */
    private void loadDict() {
        dicts.clear();
        JsonParser jsonParser = new JsonParser();

        try {
            String xmlPath = getClass().getPackage().getName().replace('.', '/');
            xmlPath = xmlPath + "/config/" + config + ".json";
            URL url = getClass().getClassLoader().getResource(xmlPath);
            File file;
            if (url.toString().startsWith("jar:")) {
                InputStream inputStream = getClass().getResourceAsStream(xmlPath);
                file = File.createTempFile("tmpfile", ".tmp");
                OutputStream outputStream = new FileOutputStream(file);

                int read;
                byte[] bytes = new byte[1024];
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }

                file.deleteOnExit();
            }
            else {
                file = new File(url.getFile());
            }

            Object object = jsonParser.parse(new FileReader(file));
            JsonObject dictRoot = (JsonObject) object;

            name = dictRoot.get("name").getAsString();
            JsonArray jsonArray = dictRoot.get("dicts").getAsJsonArray();

            Float prior = 0.1f;
            //loop over all languages
            for (Object obj : jsonArray) {

                JsonObject _dictRoot = (JsonObject) obj;
                String _name = _dictRoot.get("name").getAsString();
                String _description = _dictRoot.get("description").getAsString();
                String _files = _dictRoot.get("description").getAsString();

                Set<String> _set = new HashSet<>();



                // read all characters from the first tab-separated column of all files "dicts"
                for (JsonElement _obj : _dictRoot.get("dicts").getAsJsonArray()) {
                    xmlPath = getClass().getPackage().getName().replace('.', '/');
                    xmlPath = xmlPath + "/vocabulary/" + _name + ".voc";
                    url = getClass().getClassLoader().getResource(xmlPath);
                    try {
                        if (url.toString().startsWith("jar:")) {
                            InputStream inputStream = getClass().getResourceAsStream(xmlPath);
                            file = File.createTempFile("tmpdictfile", ".tmp");
                            OutputStream outputStream = new FileOutputStream(file);

                            int read;
                            byte[] bytes = new byte[1024];
                            while ((read = inputStream.read(bytes)) != -1) {
                                outputStream.write(bytes, 0, read);
                            }

                            file.deleteOnExit();
                        } else {
                            file = new File(url.getFile());
                        }

                        List<String> lines = Files.readAllLines(file.toPath());
                        for (String line : lines) {
                            String[] chars = line.trim().split("\t")[0].trim().split("");
                            _set.addAll(Arrays.asList(chars));
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


                dicts.put(_name, _set);
                priorities.put(_name, prior);
                names.put(_name, _description);
                prior = prior / 2;
            }

        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printStats() {
        for (String _name : dicts.keySet()) {
            System.out.println(getDescription(_name) + ": " + dicts.get(_name).size() + " symbols");
        }
    }

    private Map<String, Float> computeScores(String string) {
        String[] chars = string.trim().split("");
        Map<String, Float> scores = new HashMap<>();

        for (String _name : dicts.keySet()) {
            scores.put(_name, priorities.get(_name));
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
     *
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
        }
        return language;
    }

    public String detectDescription(String string) {
        String language = detect(string);
        return names.get(language);
    }

    public String getDescription(String language) {
        return names.get(language);
    }

    public void printSupportedLanguages() {
        for (String language : names.keySet()){
            System.err.println("supported language:" + language);
        }
    }

    public boolean support(Language language){
        String region = language.getRegion();

        if (language.getLanguage() == "zh") { //language must be zh
            if (region == null) {
                region = "TW"; //default region if not specified; override with Traditional Chinese (TW)
            }

            if (getNames().containsKey(region)) {
                return true;
            }
        }

        return false;
    }
}
