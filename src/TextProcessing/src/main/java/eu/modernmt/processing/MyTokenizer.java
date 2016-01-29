package eu.modernmt.processing;

import eu.modernmt.processing.framework.*;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.jflex.JFlexTokenizer;
import eu.modernmt.processing.tokenizer.moses.MosesTokenizer;
import eu.modernmt.processing.util.StringJoiner;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Created by davide on 28/01/16.
 */
public class MyTokenizer {


    static {
        System.setProperty("mmt.home", "/Users/davide/workspaces/mmt/ModernMT/");
    }

//    public static void main(String[] args) throws Throwable {
//        String text = "In 2009, farms with an area of up to 5 hectares made up 52,5% of all farms.";
//        MyTokenizer tokenizer = new MyTokenizer();
//
//        String[] tokens = tokenizer.call(text);
//        for (String token : tokens) {
//            System.out.println("'" + token + "'");
//        }
//
//        System.out.println(new StringJoiner().call(tokens));
//    }

//    public static void main(String[] args) throws Throwable {
//        int[] ignoreLines = new int[]{
//        };
//
//        HashSet<Integer> ignoreSet = new HashSet<>();
//        for (int i : ignoreLines)
//            ignoreSet.add(i);
//
//        UnixLineReader reader = null;
//
//        ProcessingPipeline<String, String[]> baseline = null;
//        ProcessingPipeline<String, String[]> newpipeline = null;
//
//        try {
//            reader = new UnixLineReader(new FileReader("/Users/davide/Desktop/tokenizer/detok.text.en"));
//
//            MyTokenizer myTokenizer = new MyTokenizer();
//
//            baseline = new ProcessingPipeline.Builder<String, String>()
//                    .add(new MosesTokenizer("en"))
//                    .create();
//
//            newpipeline = new ProcessingPipeline.Builder<String, String>()
//                    .add(myTokenizer)
//                    .create();
//
//            String line;
//            int SKIP = 1996600;
//            int count = 1;
//            while ((line = reader.readLine()) != null) {
//                if (ignoreSet.contains(count) || count < SKIP) {
//                    count++;
//                    continue;
//                }
//
//                String[] newpipelineTokens = newpipeline.process(line);
//                String[] baselineTokens = fixer(baseline.process(line));
//
//                if (!Arrays.equals(newpipelineTokens, baselineTokens)) {
////                    System.out.println(count + ":  '" + toString(myTokenizer.lastProtected) + "'");
//                    System.out.println(count + ": > " + toString(newpipelineTokens));
////                    System.out.println(count + ": < " + toString(baselineTokens));
//                    System.out.println();
//
//                    if (SKIP > 0)
//                        break;
//                    if (count == 1996619)
//                        break;
//                } else {
//                    System.out.println(count + ": '" + toString(newpipelineTokens) + "'");
//                }
//
//                count++;
//            }
//        } finally {
//            IOUtils.closeQuietly(reader);
//            IOUtils.closeQuietly(baseline);
//            IOUtils.closeQuietly(newpipeline);
//        }
//    }

    private static String[] fixer(String[] mosesTokens) {
        String text = toString(mosesTokens);

        // Better
        text = text.replace(".com.", ".com .");
        text = text.replace(".it.", ".it .");
        text = text.replace(".org.", ".org .");
        text = text.replace(".net.", ".net .");
        text = text.replace("`", " ` ");
        text = text.replace("+ 39", "+39");

        text = text.replace("Prof..", "Prof. .");
        text = text.replace("Prof ..", "Prof. .");
        text = text.replace("etc. .", "etc ..");
        text = text.replace("Dept .", "Dept.");
        text = text.replace("Duomo. .", "Duomo ..");

        // Acceptable
        text = text.replace("S.23.01", "S. 23.01");

        return text.split("\\s+");
    }

    public static void main(String[] args) throws Throwable {
        UnixLineReader reader = null;

        ProcessingPipeline<String, String> baseline = null;
        ProcessingPipeline<String, String> newpipeline = null;

        try {

            newpipeline = new ProcessingPipeline.Builder<String, String>()
                    .add(JFlexTokenizer.ENGLISH)
                    .add(new StringJoiner())
                    .create();

            long time = System.currentTimeMillis();
            newpipeline.processAll(PipelineInputStream.fromInputStream(new FileInputStream("/Users/davide/Desktop/tokenizer/detok.text.en")), PipelineOutputStream.fromOutputStream(new FileOutputStream("/Users/davide/Desktop/tokenizer/tok.text.en")));
            System.out.println((System.currentTimeMillis() - time) / 1000);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(baseline);
            IOUtils.closeQuietly(newpipeline);
        }
    }

    private static String toString(String[] tokens) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0)
                builder.append(' ');
            builder.append(tokens[i]);
        }
        return builder.toString();
    }

    private static String toString(boolean[] flags) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < flags.length; i++) {
            builder.append(flags[i] ? '1' : '0');
        }
        return builder.toString();
    }
}
