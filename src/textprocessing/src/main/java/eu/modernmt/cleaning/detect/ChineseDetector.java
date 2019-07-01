package eu.modernmt.cleaning.detect;
/**
 * Created by nicolabertoldi on 04/02/18.
 */


import eu.modernmt.lang.Language;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.PrimitiveIterator;

/**
 * ChineseDetector holds the vocabularies of Simplified/Traditional Chinese and variants for detecting the actual language of a text
 */
public class ChineseDetector {

    private static ChineseDetector instance = null;

    public static ChineseDetector getInstance() {
        if (instance == null) {
            synchronized (ChineseDetector.class) {
                if (instance == null)
                    instance = new ChineseDetector();
            }
        }

        return instance;
    }

    private final HashSet<Integer> simplifiedVoc;
    private final HashSet<Integer> traditionalVoc;

    private ChineseDetector() {
        // load all Simplified-only Chinese character
        simplifiedVoc = loadDict("CN");

        // load all Traditional-only Chinese character
        traditionalVoc = loadDict("TW");
    }

    /**
     * Load dictionary files into a Set
     */
    private HashSet<Integer> loadDict(String region) {
        HashSet<Integer> voc = new HashSet<>();
        String filename = region + ".voc";
        BufferedReader in = null;

        try {
            String xmlPath = getClass().getPackage().getName().replace('.', '/');
            xmlPath = xmlPath + "/" + filename;
            URL url = getClass().getClassLoader().getResource(xmlPath);
            if (url == null)
                throw new IOException("Vocabulary " + filename + " does not exist.");

            in = new BufferedReader(new InputStreamReader(url.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                PrimitiveIterator.OfInt stream = inputLine.codePoints().iterator();
                while (stream.hasNext()) {
                    int codepoint = stream.nextInt();
                    voc.add(codepoint);
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            IOUtils.closeQuietly(in);
        }

        return voc;
    }

    /**
     * Detects the language of the string
     *
     * @param string input string to convert
     * @return language
     */
    public Language detect(String string) {
        int simplifiedCount = 0, traditionalCount = 0;
        PrimitiveIterator.OfInt stream = string.codePoints().iterator();

        while (stream.hasNext()) {
            int codepoint = stream.nextInt();
            if (simplifiedVoc.contains(codepoint)) {
                simplifiedCount++;
            }
            if (traditionalVoc.contains(codepoint)) {
                traditionalCount++;
            }
        }

        if (simplifiedCount == traditionalCount)
            return Language.CHINESE;
        else if (simplifiedCount > traditionalCount)
            return Language.CHINESE_SIMPLIFIED;
        else // (traditionalCount > simplifiedCount)
            return Language.CHINESE_TRADITIONAL;
    }

}

