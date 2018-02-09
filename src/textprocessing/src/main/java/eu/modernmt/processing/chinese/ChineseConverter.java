package eu.modernmt.processing.chinese;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

/**
 * ChineseConverter converts Simplified Chinese to Traditional Chinese (including Taiwan and Hong Konf standard) and vice versa
 */
public class ChineseConverter {

    private static ChineseConverter cntwInstance = null;
    private static ChineseConverter twcnInstance = null;

    public static ChineseConverter getSimplifiedToTraditionalConverter() {
        if (cntwInstance == null) {
            synchronized (ChineseConverter.class) {
                if (cntwInstance == null)
                    cntwInstance = newInstance("CN-TW.map");
            }
        }

        return cntwInstance;
    }

    public static ChineseConverter getTraditionalToSimplifiedConverter() {
        if (twcnInstance == null) {
            synchronized (ChineseConverter.class) {
                if (twcnInstance == null)
                    twcnInstance = newInstance("TW-CN.map");
            }
        }

        return twcnInstance;
    }

    private static ChineseConverter newInstance(String filename) {
        String basepath = ChineseConverter.class.getPackage().getName().replace('.', '/');
        String path = basepath + "/mapping/" + filename;

        URL url = ChineseConverter.class.getClassLoader().getResource(path);
        if (url == null)
            throw new Error("Unable to locate resource: " + path);

        InputStream input = null;

        try {
            input = url.openStream();

            List<String> lines = IOUtils.readLines(input, "UTF-8");
            HashMap<Character, Character> dictionary = new HashMap<>(lines.size());

            for (String line : lines) {
                String[] parts = line.split("\t");
                String key = parts[0];
                String value = parts[1].split(" ")[0];

                if (key.length() > 1 || value.length() > 1) {
                    System.out.println("'" + key + "'(" + key.length() + ") > '" + value + "'(" + value.length() + ")");
                    if (key.length() > 1)
                        printSurrogate(key);
                    if (value.length() > 1)
                        printSurrogate(value);
                }

                dictionary.put(line.charAt(0), line.charAt(1));
            }

            return new ChineseConverter(dictionary);
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    private final HashMap<Character, Character> dictionary;

    private ChineseConverter(HashMap<Character, Character> dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * convert the string
     *
     * @param string input string to convert
     * @return converted string
     */
    public String convert(String string) {
        char[] converted = null;

        for (int i = 0; i < string.length(); ++i) {
            Character c = dictionary.get(string.charAt(i));

            if (c != null) {
                if (converted == null)
                    converted = string.toCharArray();
                converted[i] = c;
            }
        }

        return converted == null ? string : new String(converted);
    }


    private static void printSurrogate(String string) {
        if (string.length() != 2)
            throw new RuntimeException(string + " of length " + string.length());

        System.out.println(string + " " + Character.isSurrogate(string.charAt(0)) + " " + Character.isSurrogate(string.charAt(1)));
        System.out.println(string + " " + Character.isSurrogatePair(string.charAt(0), string.charAt(1)));
    }

    public static void main(String[] args) {
        ChineseConverter cn2tw = getSimplifiedToTraditionalConverter();
        ChineseConverter tw2cn = getTraditionalToSimplifiedConverter();

        int i;

        char[] cnChars = new char[cn2tw.dictionary.size()];
        i = 0;
        for (char c : cn2tw.dictionary.keySet())
            cnChars[i++] = c;

        char[] twChars = new char[tw2cn.dictionary.size()];
        i = 0;
        for (char c : tw2cn.dictionary.keySet())
            twChars[i++] = c;

        String cn = new String(cnChars);
        String cn_tw = cn2tw.convert(cn);
        String cn_tw_cn = tw2cn.convert(cn_tw);

        String tw = new String(twChars);
        String tw_cn = tw2cn.convert(tw);
        String tw_cn_tw = cn2tw.convert(tw_cn);

//        for (int j = 0; j < cn.length(); j++) {
//            if (cn.charAt(j) != cn_tw_cn.charAt(j))
//                System.out.println(cn.charAt(j) + " != " + cn_tw_cn.charAt(j));
//        }
//
//        System.out.println(cn.equals(cn_tw_cn));

//        for (int j = 0; j < tw.length(); j++) {
//            if (tw.charAt(j) != tw_cn_tw.charAt(j))
//                System.out.println(tw.charAt(j) + " != " + tw_cn_tw.charAt(j));
//        }
//
//        System.out.println(tw.equals(tw_cn_tw));

    }
}
