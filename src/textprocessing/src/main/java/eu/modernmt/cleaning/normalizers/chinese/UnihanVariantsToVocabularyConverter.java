package eu.modernmt.cleaning.normalizers.chinese;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by davide on 12/02/18.
 */
public class UnihanVariantsToVocabularyConverter {

    public static void main(String[] args) throws Throwable {
        File file = new File(args[0]);

        HashSet<Integer> cn = new HashSet<>();
        HashSet<Integer> tw = new HashSet<>();

        // Read all lines
        ArrayList<ParsedLine> lines = new ArrayList<>();
        for (String line : FileUtils.readLines(file, "UTF-8")) {
            ParsedLine e = ParsedLine.parse(line);
            if (e != null)
                lines.add(e);
        }

        // Split chars for 'kTraditionalVariant' and 'kSimplifiedVariant'
        for (ParsedLine line : lines) {
            if (line.is("kTraditionalVariant")) {
                cn.add(line.left);
                for (int c : line.right)
                    tw.add(c);
            } else if (line.is("kSimplifiedVariant")) {
                tw.add(line.left);
                for (int c : line.right)
                    cn.add(c);
            }
        }

        // Re-group chars by semantic variant
        boolean hasChanged;

        do {
            hasChanged = false;

            for (ParsedLine line : lines) {
                if (line.is("kSemanticVariant", "kSpecializedSemanticVariant", "kZVariant")) {
                    int[] codepoints = line.getAllCodepoints();

                    if (containsAny(codepoints, cn))
                        hasChanged |= addToSet(codepoints, cn);

                    if (containsAny(codepoints, tw))
                        hasChanged |= addToSet(codepoints, tw);
                }
            }
        } while (hasChanged);

        // Strip intersection
        HashSet<Integer> intersection = new HashSet<>(cn);
        intersection.retainAll(tw);

        cn.removeAll(intersection);
        tw.removeAll(intersection);

        // Save vocabularies
        toFile(cn, new File(args[1], "cn.txt"));
        toFile(tw, new File(args[1], "tw.txt"));
    }

    private static boolean addToSet(int[] codepoints, HashSet<Integer> set) {
        boolean hasChanged = false;
        for (int codepoint : codepoints) {
            if (!set.contains(codepoint)) {
                hasChanged = true;
                set.add(codepoint);
            }
        }

        return hasChanged;
    }

    private static boolean containsAny(int[] codepoints, HashSet<Integer> set) {
        for (int codepoint : codepoints) {
            if (set.contains(codepoint))
                return true;
        }

        return false;
    }

    private static void toFile(HashSet<Integer> codepoints, File file) throws IOException {
        Writer writer = null;

        try {
            writer = new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8");

            StringBuilder str = new StringBuilder();
            for (int codepoint : codepoints) {
                str.setLength(0);
                str.appendCodePoint(codepoint);
                str.append('\n');

                writer.append(str);
            }
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private static class ParsedLine {

        public final int left;
        public final String variant;
        public final int[] right;

        public static ParsedLine parse(String line) {
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == '#')
                return null;

            String[] parts = line.split("\\s+", 3);
            if (parts.length != 3)
                throw new RuntimeException("Invalid length: " + line);

            int left = toCodePoint(parts[0]);
            int[] right = toCodePoints(parts[2]);

            return new ParsedLine(left, parts[1], right);
        }

        private static int toCodePoint(String string) {
            if (!string.matches("U\\+[0-9A-F]+"))
                throw new IllegalArgumentException("Ivalid codepoint: " + string);

            return Integer.parseInt(string.substring(2), 16);
        }

        private static int[] toCodePoints(String string) {
            String[] parts = string.split("\\s+");
            int[] result = new int[parts.length];

            for (int i = 0; i < result.length; i++) {
                String part = parts[i].split("<")[0];
                result[i] = toCodePoint(part);
            }

            return result;
        }

        private ParsedLine(int left, String variant, int[] right) {
            this.left = left;
            this.variant = variant;
            this.right = right;
        }

        public boolean is(String... variants) {
            for (String variant : variants) {
                if (variant.equals(this.variant))
                    return true;
            }

            return false;
        }

        public int[] getAllCodepoints() {
            int[] result = new int[right.length + 1];
            System.arraycopy(right, 0, result, 1, right.length);
            result[0] = left;
            return result;
        }
    }
}
