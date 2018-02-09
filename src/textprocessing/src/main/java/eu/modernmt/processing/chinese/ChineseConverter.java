package eu.modernmt.processing.chinese;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * ChineseConverter converts Simplified Chinese to Traditional Chinese (including Taiwan and Hong Konf standard) and vice versa
 */
public class ChineseConverter {

    protected String name;
    protected String config;
    private static final  Map<Character, Character> dictionary = new TreeMap<>();

    /**
     * construct ChineseConverter with default config of "CN-TW"
     */
    public ChineseConverter() throws IOException, UnsupportedLanguageException {
        this(new Language("zh","CN"),new Language("zh","TW")); //the default conversion is from Simplified CHinese (CN) to Traditional Chinese (TW)
    }

    /**
     * construct ChineseConverter with conversion
     *
     * @param src language for converting into
     * @param trg language for converting into
     */
    public ChineseConverter(Language src, Language trg) throws IOException, UnsupportedLanguageException {

        String srcRegion = src.getRegion();
        String trgRegion = trg.getRegion();
        if (srcRegion == null){ srcRegion = "CN"; }
        if (trgRegion == null){ trgRegion = "TW"; }

        name = srcRegion + "-" + trgRegion;
        if ( ! name.equals("CN-TW") && ! name.equals("TW-CN") ){
            throw new UnsupportedLanguageException(src,trg);
        }
        this.config = "";

        if (dictionary.size() == 0) {
            setConfig(name);
        }
    }

    /**
     * @return dict name
     */
    public String getDictName() {
        return name;
    }

    /**
     * set ChineseConverter a new conversion
     *
     * @param conversion options are "CN-TW" or "TW-CN"
     */
    public void setConversion(String conversion) throws IOException {
       setConfig(conversion);
    }

    /**
     * set config
     * @param config the config to use, including "CN-TW" or "TW-CN"
     */
    public void setConfig(String config) throws IOException {
        config = config.toUpperCase();

        if (this.config.equals(config)) {
            return;
        }
        this.config = config;

        loadDict();
    }


    /**
     * load dictionary files into dictChain
     */
    private void loadDict() throws IOException{
        dictionary.clear();

        try {
            String xmlPath = getClass().getPackage().getName().replace('.', '/');
            xmlPath = xmlPath + "/config/" + config + ".json";
            URL url = getClass().getClassLoader().getResource(xmlPath);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String _name = inputLine.trim();
                xmlPath = getClass().getPackage().getName().replace('.', '/');
                xmlPath = xmlPath + "/mapping/" + _name;
                url = getClass().getClassLoader().getResource(xmlPath);
                BufferedReader mapReader = new BufferedReader(new InputStreamReader(url.openStream()));

                Set<Character> _set = new HashSet<>();
                while ((inputLine = mapReader.readLine()) != null) {
                    String[] words = inputLine.trim().split("\t");
                    dictionary.put(words[0].charAt(0), words[1].charAt(0));
                    _set.add(inputLine.charAt(0));
                }
                mapReader.close();
            }
            in.close();
        } catch (MalformedURLException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * convert the string
     *
     * @param string input string to convert
     * @return converted string
     */
    public String convert(String string) {
        if (string.length() == 0) {
            return "";
        }

        StringBuilder outputStringBuilder = new StringBuilder(string);
        for (int i = 0; i < string.length(); ++i){
            Character c = string.charAt(i);
            if (dictionary.containsKey(c)) {
                outputStringBuilder.setCharAt(i, dictionary.get(c));
            }
        }

        return outputStringBuilder.toString();
    }

    public static void main(String[] args) throws IOException {

        Language srcLanguage, trgLanguage;
        srcLanguage = new Language("zh",args[0]);
        trgLanguage = new Language("zh",args[1]);
        System.err.println("converting from " + srcLanguage + " to " + trgLanguage);

        ChineseConverter converter = new ChineseConverter(srcLanguage,trgLanguage);

        Scanner in = new Scanner(System.in);

        while(in.hasNext()) {
            String from = in.next();
            System.out.println(converter.convert(from));
        }
    }
}
