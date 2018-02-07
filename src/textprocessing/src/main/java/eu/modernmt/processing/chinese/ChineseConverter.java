package eu.modernmt.processing.chinese;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

/**
 * ChineseConverter converts Simplified Chinese to Traditional Chinese (including Taiwan and Hong Konf standard) and vice versa
 */
public class ChineseConverter {

    protected String name;
    protected String description;
    protected String config;
    protected Map<String, String> dictionary;


    /**
     * construct ChineseConverter with default config of "CN2TW"
     */
    public ChineseConverter() throws IOException {
        this("CN-TW");
    }

    /**
     * construct ChineseConverter with conversion
     *
     * @param conversion options are "CN-TW" or "TW-CN"
     */
    public ChineseConverter(String conversion) throws IOException {
        dictionary = new TreeMap<>();
        name = conversion;
        this.config = "";
        setConfig(name);
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
     * @param config the config to use, including "hk2s", "s2hk", "s2t", "s2tw", "s2twp",
     *               "t2hk", "t2s", "t2tw", "tw2s", and "tw2sp"
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
        JsonParser jsonParser = new JsonParser();

        String filename = "";

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
            description = dictRoot.get("description").getAsString();
            filename = dictRoot.get("dict").getAsString();

        } catch (JsonSyntaxException e) {
            throw new Error("Json format error",e);
        }

        try {
            String xmlPath = getClass().getPackage().getName().replace('.', '/');
            xmlPath = xmlPath + "/mapping/" + filename;
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
            } else {
                file = new File(url.getFile());
            }

            List<String> lines = Files.readAllLines(file.toPath());

            for (String line : lines) {
                String[] words = line.trim().split("\t");
                dictionary.put(words[0], words[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            String c = string.substring(i,i+1);
            if (dictionary.containsKey(c)) {
                outputStringBuilder.replace(i,i+1,dictionary.get(c));
            }
        }

        return outputStringBuilder.toString();
    }

    public static void main(String[] args) throws IOException {
        String targetLanguage = "TW";   //internal language is Traditional Chinese (TW)

        ChineseConverter conv = new ChineseConverter();

        Map<String, ChineseConverter> converters = new HashMap<>();
        ChineseDetector detector = new ChineseDetector();

        String sourceLanguage;
        String to;

        String[] fromList = {"开放中文转换", "開放中文轉換", "偽", "香菸（英語：Cigarette，為菸草製品的一種。滑鼠是一種很常見及常用的電腦輸入裝置"};

        for (String from : fromList) {
            sourceLanguage = detector.detect(from);
            String conversion = sourceLanguage + "-" + targetLanguage;

            if (sourceLanguage.equals(targetLanguage)) {
                to = from;
            } else {
                if (!converters.containsKey(conversion)) {
                    converters.put(conversion, new ChineseConverter(conversion));
                }
                ChineseConverter converter = converters.get(conversion);
                to = converter.convert(from);
            }
            System.out.println("from:" + from + " to:" + to + "\n");
        }
    }
}
