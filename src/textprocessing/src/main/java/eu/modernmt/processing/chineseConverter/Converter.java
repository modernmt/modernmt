package eu.modernmt.processing.chineseConverter;

import java.util.Map;

/**
 * Converter converts Simplified Chinese to Traditional Chinese (including Taiwan and Hong Konf standard) and vice versa
 */
public class Converter {

    Dictionary dictionary;
    protected boolean verbose = false;

    public void setVerbose(boolean verbose) {
        dictionary.setVerbose(verbose);
        this.verbose = verbose;
    }

    /**
     * construct Converter with default config of "s2t"
     */
    public Converter() {
        this("s2t");
    }

    /**
     * construct Converter with conversion
     * @param conversion options are "hk2s", "s2hk", "s2t", "s2tw", "s2twp", "t2hk", "t2s",
     *               "t2tw", "tw2s", and "tw2sp"
     */
    public Converter(String conversion) { dictionary = new Dictionary(conversion); }

    /**
     *
     * @return dict name
     */
    public String getDictName() {
        return dictionary.getDictName();
    }

    /**
     * set Converter a new conversion
     * @param conversion options are "hk2s", "s2hk", "s2t", "s2tw", "s2twp", "t2hk", "t2s",
     *               "t2tw", "tw2s", and "tw2sp"
     */
    public void setConversion(String conversion) {
        dictionary.setConfig(conversion);
    }


    /**
     * convert the string
     * @param string input string to convert
     * @return converted string
     */
    public String convert(String string) {
        if (string.length() == 0) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder(string);

        for (Map<String, String> dictMap : dictionary.getDictChain()) {
            for (String key : dictMap.keySet()) {
                int fromIndex = 0;
                int pos = stringBuilder.indexOf(key, fromIndex);
                while (pos >= 0) {
                    String converted = dictMap.get(key);
                    converted = converted.split(" ")[0];  // get the 1st result if multiple choices available
                    stringBuilder.replace(pos, pos + key.length(), converted);
                    fromIndex = pos + converted.length();
                    pos = stringBuilder.indexOf(key, fromIndex);
                }
            }
        }

        return stringBuilder.toString();
    }

}
