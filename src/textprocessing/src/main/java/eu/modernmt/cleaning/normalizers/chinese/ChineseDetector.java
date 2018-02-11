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
import java.util.Scanner;
import java.util.Set;

/**
 * ChineseDetector holds the vocabularies of Simplified/Traditional Chinese and variants for detecting the actual language of a text
 */
public class ChineseDetector {
    private static final Set<Integer> simplifiedVoc = new HashSet<>();
    private static final Set<Integer> traditionalVoc = new HashSet<>();

    public ChineseDetector() {
        // load all "characters" from the Simplified Chinese vocabulary
        if (simplifiedVoc.size() == 0) {
            loadDict(new Language("zh", "CN"), simplifiedVoc);
        }

        // load all "characters" from the Traditional Chinese vocabulary
        if (traditionalVoc.size() == 0) {
            loadDict(new Language("zh", "TW"), traditionalVoc);
        }

        // remove chars which are in both Simplified and Traditional Chinese vocabularies
        if ( ( simplifiedVoc.size() > 0 ) && ( traditionalVoc.size() > 0 ) ) {
            for (Object c : simplifiedVoc.toArray()) {
                if (traditionalVoc.contains(c)) {
                    simplifiedVoc.remove(c);
                    traditionalVoc.remove(c);
                }
            }
        }
    }

    /**
     * load dictionary files into a map of Sets
     */
    private void loadDict(Language language, Set<Integer> voc) {
        voc.clear();
        String filename = language.getRegion() + ".voc";
        try {
            String xmlPath = getClass().getPackage().getName().replace('.', '/');
            xmlPath = xmlPath + "/" + filename;
            URL url = getClass().getClassLoader().getResource(xmlPath);
            if (url == null) {
                throw new IOException("Vocabulary " + filename + " does not exist.");
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

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
        }
    }

    public void printStats() {
        System.out.println("Simplified Chinese vocabulary : " + simplifiedVoc.size() + " symbols");
        System.out.println("Traditional Chinese vocabulary: " + traditionalVoc.size() + " symbols");
    }


    /**
     * detect the language of the string
     *
     * @param string input string to convert
     * @return region
     */
    private String detectRegion(String string) {
        int simplifiedCount = 0, traditionalCount = 0;
        PrimitiveIterator.OfInt stream = string.codePoints().iterator();
        while (stream.hasNext()) {
            int codepoint = stream.nextInt();
            if (simplifiedVoc.contains(codepoint)) { simplifiedCount++; }
            if (traditionalVoc.contains(codepoint)) { traditionalCount++; }
        }
        if (simplifiedCount > traditionalCount) { return "CN"; }
        else if (traditionalCount > 0) { return "TW"; }
        else { return null; }
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

    public static void main(String[] args) throws IOException {
        ChineseDetector detector = new ChineseDetector();
        detector.printStats();

        Scanner in = new Scanner(System.in);
        while (in.hasNext()) {
            String inputLine = in.next();
            System.out.print("language:" + detector.detectLanguage(inputLine) + " for string " + inputLine);
        }
    }
}

