package eu.modernmt.cleaning.normalizers.chinese;
/**
 * Created by nicolabertoldi on 04/02/18.
 */


import eu.modernmt.lang.Language;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.PrimitiveIterator;
import java.util.Set;

/**
 * ChineseDetector holds the vocabularies of Simplified/Traditional Chinese and variants for detecting the actual language of a text
 */
public class ChineseDetector {
    private static Set<Integer> simplifiedVoc = null;
    private static Set<Integer> traditionalVoc = null;

    private static ChineseDetector instance = null;

    public static ChineseDetector getInstance() throws IOException {
        if (instance == null) {
            synchronized (ChineseDetector.class) {
                if (instance == null)
                    instance = new ChineseDetector();
            }
        }

        return instance;
    }

    private ChineseDetector() throws IOException {
        // load all Simplified-only Chinese character
        if (simplifiedVoc == null) {
            simplifiedVoc = loadDict("CN");
        }

        // load all Traditional-only Chinese character
        if (traditionalVoc == null) {
            traditionalVoc = loadDict("TW");
        }
    }

    /**
     * load dictionary files into a map of Sets
     */
    private Set<Integer> loadDict(String region) throws IOException {
        Set<Integer> voc = new HashSet<>();
        String filename = region + ".voc";
        BufferedReader in = null;
        try {
            String xmlPath = getClass().getPackage().getName().replace('.', '/');
            xmlPath = xmlPath + "/" + filename;
            URL url = getClass().getClassLoader().getResource(xmlPath);
            if (url == null) {
                throw new IOException("Vocabulary " + filename + " does not exist.");
            }
            in = new BufferedReader(new InputStreamReader(url.openStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                PrimitiveIterator.OfInt stream = inputLine.codePoints().iterator();
                while (stream.hasNext()) {
                    int codepoint = stream.nextInt();
                    voc.add(codepoint);
                }
            }
            in.close();

        } catch (IOException e) {
            throw new Error(e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return voc;
    }

    public void printStats() {
        System.err.println("Simplified-only Chinese vocabulary : " + simplifiedVoc.size() + " symbols");
        System.err.println("Traditional-only Chinese vocabulary: " + traditionalVoc.size() + " symbols");
    }


    /**
     * detect the language of the string
     *
     * @param string input string to convert
     * @return language
     */
    public Language detectLanguage(String string) {
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
        if (simplifiedCount > traditionalCount) { return Language.CHINESE_SIMPLIFIED; }
        else if (traditionalCount > 0) { return Language.CHINESE_TRADITIONAL;  }
        else {return Language.CHINESE;  }
    }

    //    public Language detectLanguage(String string) {
    public String scoreLanguage(String string) {
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
        return simplifiedCount + " " + traditionalCount;
    }

    public static void main(String[] args) throws IOException {
        ChineseDetector detector = ChineseDetector.getInstance();
        detector.printStats();

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(System.in));
            String inputLine;
            //loop forever an stream interruption is caught
            //in case of empty line it returns an empty line
            while ((inputLine = in.readLine()) != null) {
                System.out.println(detector.detectLanguage(inputLine) + " " + inputLine);
                System.out.println(detector.scoreLanguage(inputLine) + " " + inputLine);
            }
        } finally {
            if (in != null){
                in.close();
            }
        }
    }
}

