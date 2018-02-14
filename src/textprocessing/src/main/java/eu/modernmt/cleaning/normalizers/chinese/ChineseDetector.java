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
    private static Set<Integer> simplifiedIssues = null;
    private static Set<Integer> traditionalIssues = null;

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

        printStats();

        //Removed common characters
        Set<Integer> intersection = new HashSet<>(simplifiedVoc);
        intersection.retainAll(traditionalVoc);
        simplifiedVoc.removeAll(intersection);
        traditionalVoc.removeAll(intersection);

        printStats();

        if (simplifiedIssues == null) {
            simplifiedIssues = new HashSet<>();
        }
        if (traditionalIssues == null) {
            traditionalIssues = new HashSet<>();
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

    public void printVoc() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Integer c : simplifiedVoc){
            stringBuilder.append("cn:|");
            stringBuilder.appendCodePoint(c);
            stringBuilder.append("|\n");
        }
        System.err.println("### Simplified-only Chinese vocabulary)");
        System.err.println(stringBuilder.toString());

        stringBuilder.setLength(0);
        for (Integer c : traditionalVoc){
            stringBuilder.append("tw:");
            stringBuilder.appendCodePoint(c);
            stringBuilder.append('\n');
        }
        System.err.println("### Traditional-only Chinese vocabulary)");
        System.err.println(stringBuilder.toString());
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
        if (simplifiedCount > traditionalCount) {
            return Language.CHINESE_SIMPLIFIED;
        } else if (traditionalCount > 0) {
            return Language.CHINESE_TRADITIONAL;
        } else {
            return Language.CHINESE;
        }
    }

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

    public void setProblems(String string) {
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

        if (traditionalCount < simplifiedCount && traditionalCount == 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stream = string.codePoints().iterator();
            while (stream.hasNext()) {
                int codepoint = stream.nextInt();
                if (traditionalVoc.contains(codepoint)) {
                    traditionalIssues.add(codepoint);
                    stringBuilder.appendCodePoint(codepoint);
                    stringBuilder.append(' ');
                }
            }
            System.out.println(simplifiedCount + " " + traditionalCount + " TW issue: " +stringBuilder.toString() + " sentence:" + string);
        }

        if (simplifiedCount < traditionalCount && simplifiedCount == 1) {
            stream = string.codePoints().iterator();
            StringBuilder stringBuilder = new StringBuilder();
            while (stream.hasNext()) {
                int codepoint = stream.nextInt();
                if (simplifiedVoc.contains(codepoint)) {
                    simplifiedIssues.add(codepoint);
                    stringBuilder.appendCodePoint(codepoint);
                    stringBuilder.append(' ');
                }
            }
            System.out.println(simplifiedCount + " " + traditionalCount + " CN issue: " + stringBuilder.toString() + " sentence:" + string);
        }
    }



    public static void main(String[] args) throws IOException {
        ChineseDetector detector = ChineseDetector.getInstance();
//        detector.printVoc();

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(System.in));
            String inputLine;
            //loop forever an stream interruption is caught
            //in case of empty line it returns an empty line
            while ((inputLine = in.readLine()) != null) {
//                System.out.println(detector.detectLanguage(inputLine) + " " + inputLine);
                System.out.println(detector.scoreLanguage(inputLine) + " " + inputLine);
//                detector.setProblems(inputLine);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        if (false) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Integer c : simplifiedIssues) {
                stringBuilder.appendCodePoint(c);
                stringBuilder.append('\n');
            }
            System.out.println("### Simplified Issues");
            System.out.println(stringBuilder.toString());

            stringBuilder = new StringBuilder();
            for (Integer c : traditionalIssues) {
                stringBuilder.appendCodePoint(c);
                stringBuilder.append('\n');
            }
            System.out.println("### Traditional Issues");
            System.out.println(stringBuilder.toString());
        }
    }

//
//    public static void main(String[] args) throws IOException {
//
//        Set<Integer> issues = new HashSet<>();
//
//        String xmlPath = "/Users/nicolabertoldi/Work/Software/ModernMT/GitHubRepository/ModernMT_MAC/src/textprocessing/src/main/resources/eu/modernmt/cleaning/normalizers/chinese/ISSUES.txt";
//
//        BufferedReader inIssues = null;
//        try {
//            inIssues = new BufferedReader(new InputStreamReader(new FileInputStream(xmlPath)));
//            String inputLine;
//            //loop forever an stream interruption is caught
//            //in case of empty line it returns an empty line
//            PrimitiveIterator.OfInt stream = null;
//            StringBuilder sb = new StringBuilder();
//            while ((inputLine = inIssues.readLine()) != null) {
//                stream = inputLine.codePoints().iterator();
//                sb.setLength(0);
//                while (stream.hasNext()) {
//                    Integer codepoint = stream.nextInt();
//                    issues.add(codepoint);
//                }
//            }
//        } finally {
//            if (inIssues != null) {
//                inIssues.close();
//            }
//        }
//
//        BufferedReader in = null;
//        try {
//            in = new BufferedReader(new InputStreamReader(System.in));
//            String inputLine;
//            StringBuilder sb = new StringBuilder();
//            //loop forever an stream interruption is caught
//            //in case of empty line it returns an empty line
//            while ((inputLine = in.readLine()) != null) {
//                PrimitiveIterator.OfInt stream = null;
//                stream = inputLine.codePoints().iterator();
//                sb.setLength(0);
//                while (stream.hasNext()) {
//                    Integer codepoint = stream.nextInt();
//                    sb.setLength(0);
//                    sb.appendCodePoint(codepoint);
//                    if (!issues.contains(codepoint)) {
//                        System.out.println(sb.toString());
//                    }
//                }
//            }
//        } finally {
//            if (in != null) {
//                in.close();
//            }
//        }
//    }
}

