package eu.modernmt.processing.chinese;
/**
 * Created by nicolabertoldi on 04/02/18.
 */


import eu.modernmt.lang.Language;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ChineseDetector holds the vocabularies of Simplified/Traditional Chinese and variants for detecting the actual language of a text
 */
public class ChineseDetector {
    private static final Map<String, Set<Character>> dicts = new HashMap<>();
    private static final Map<String, Float> priorities = new HashMap<>();

    public ChineseDetector() {
        if (dicts.size() == 0) {
            loadDict();
        }
    }

    /**
     * load dictionary files into a map of Sets
     */
    private void loadDict() {
        dicts.clear();

        Map<String, String> vocabularies = new HashMap<>();
        try {
            String xmlPath = getClass().getPackage().getName().replace('.', '/');
            xmlPath = xmlPath + "/config/detector.json";
            URL url = getClass().getClassLoader().getResource(xmlPath);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] fields = inputLine.split(" = ");
                vocabularies.put(fields[0], fields[1]);
            }
            in.close();
        } catch (MalformedURLException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }

        try {
            Float prior = 0.1f;
            for (String _name : vocabularies.keySet()) {
                String xmlPath = getClass().getPackage().getName().replace('.', '/');
                xmlPath = xmlPath + "/vocabulary/" + vocabularies.get(_name);
                URL url = getClass().getClassLoader().getResource(xmlPath);
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

                Set<Character> _set = new HashSet<>();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    _set.add(inputLine.charAt(0));
                }

                dicts.put(_name, _set);
                priorities.put(_name, prior);
                prior = prior / 2;

            }
        } catch (MalformedURLException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public void printStats() {
        for (String _name : dicts.keySet()) {
            System.out.println(_name + ": " + dicts.get(_name).size() + " symbols");
        }
    }

    private Map<String, Float> computeScores(String string) {
        char[] chars = string.toCharArray();
        Map<String, Float> scores = new HashMap<>();

        for (String _name : dicts.keySet()) {
            scores.put(_name, priorities.get(_name));
            scores.put(_name, 0.0f);
            for (Character c : chars) {
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
     * @return region
     */
    public String detectRegion(String string) {
        Map<String, Float> scores = computeScores(string);

        String region = "";
        Float best = -1000.1f;
        for (String _name : scores.keySet()) {
            Float s = scores.get(_name) + priorities.get(_name);
            if (s > best) {
                best = s;
                region = _name;
            }
        }
        return region;
    }


    /**
     * detect the language of the string
     *
     * @param string input string to convert
     * @return language
     */
    public Language detectLanguage(String string) {
        return new Language("zh", detectRegion(string));
    }

    public void printSupportedLanguages() {
        for (String language : dicts.keySet()){
            System.out.println("supported language:" + language);
        }
    }

    public boolean support(Language language) {

        if ( ! language.getLanguage().equals("zh")) { //language must be zh
            return false;
        }

        String region = language.getRegion();
        if (region == null) {
            region = "TW"; //region is not specified; override with the defaultTraditional Chinese (TW)
        }

        if ( ! dicts.containsKey(region)) {
            return false;
        }

        return true;
    }

    public static void main(String[] args) throws IOException {
        ChineseDetector detector = new ChineseDetector();

        String[] strings = {"开放中文转换", "開放中文轉換", "偽", "香菸（英語：Cigarette", "為菸草製品的一種。滑鼠是一種很常見及", "常用的電腦輸入裝置"};

        for (String string : strings) {
            System.out.println("language:" + detector.detectLanguage(string) + " for string " + string);
        }
    }
}
