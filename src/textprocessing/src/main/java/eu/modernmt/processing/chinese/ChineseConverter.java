package eu.modernmt.processing.chinese;

import java.util.HashMap;
import java.util.Map;

/**
 * ChineseConverter converts Simplified Chinese to Traditional Chinese (including Taiwan and Hong Konf standard) and vice versa
 */
public class ChineseConverter {

    Dictionary dictionary;
    protected boolean verbose = false;

    public void setVerbose(boolean verbose) {
        dictionary.setVerbose(verbose);
        this.verbose = verbose;
    }

    /**
     * construct ChineseConverter with default config of "s2t"
     */
    public ChineseConverter() {
        this("s2t");
    }

    /**
     * construct ChineseConverter with conversion
     *
     * @param conversion options are "hk2s", "s2hk", "s2t", "s2tw", "s2twp", "t2hk", "t2s",
     *                   "t2tw", "tw2s", and "tw2sp"
     */
    public ChineseConverter(String conversion) {
        dictionary = new Dictionary(conversion);
    }

    /**
     * @return dict name
     */
    public String getDictName() {
        return dictionary.getDictName();
    }

    /**
     * set ChineseConverter a new conversion
     *
     * @param conversion options are "hk2s", "s2hk", "s2t", "s2tw", "s2twp", "t2hk", "t2s",
     *                   "t2tw", "tw2s", and "tw2sp"
     */
    public void setConversion(String conversion) {
        dictionary.setConfig(conversion);
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

    public static void main(String[] args) {
        String targetLanguage = "s";   //simplified

        Map<String, ChineseConverter> converters = new HashMap<>();
        ChineseDetector detector = new ChineseDetector();

//        String from;
//        from = "开放中文转换"; //simplified
//        from = "開放中文轉換"; //traditional
//        from = "偽"; //tw or hk
//        from = "香菸（英語：Cigarette，為菸草製品的一種。滑鼠是一種很常見及常用的電腦輸入裝置";
        String sourceLanguage, conversion;
        String to;

        String[] fromList = {"开放中文转换", "開放中文轉換", "偽", "香菸（英語：Cigarette，為菸草製品的一種。滑鼠是一種很常見及常用的電腦輸入裝置"};

        for (String from : fromList) {
            sourceLanguage = detector.detect(from);
            conversion = sourceLanguage + "2" + targetLanguage;

            System.err.println("conversion:" + conversion);

            if (sourceLanguage.equals(targetLanguage)) {
                to = from;
            } else {
                if (!converters.containsKey(conversion)) {
                    converters.put(conversion, new ChineseConverter(conversion));
                }
                ChineseConverter converter = converters.get(conversion);
                to = converter.convert(from);
            }
            System.out.println(to);
        }
    }
}
