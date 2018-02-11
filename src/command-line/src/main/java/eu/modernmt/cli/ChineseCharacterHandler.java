package eu.modernmt.cli;

import java.util.PrimitiveIterator;
import java.util.Scanner;

/**
 * Created by nicolabertoldi on 11/02/18.
 */
public class ChineseCharacterHandler {
    private String separator = " ";

    public ChineseCharacterHandler() {}

    private String codepointToString(int codepoint) {
        StringBuilder sb = new StringBuilder();
        if (Character.isBmpCodePoint(codepoint)) {
            sb.append((char) codepoint);
        } else if (Character.isValidCodePoint(codepoint)) {
            sb.append(Character.highSurrogate(codepoint));
            sb.append(Character.lowSurrogate(codepoint));
        } else {
            throw new Error("Unknown codepoint " + codepoint);
        }
        return sb.toString();
    }

    public String splitAll(String string) {
        return splitAll(string, this.separator);
    }

    public String splitAll(String string, String sep) {
        StringBuilder sb = new StringBuilder();
        PrimitiveIterator.OfInt stream = string.codePoints().iterator();
        while (stream.hasNext()) {
            sb.append(codepointToString(stream.nextInt()) + sep);
        }
        return sb.toString();

    }

    public static void main(String[] args) throws Throwable{
        ChineseCharacterHandler handler = new ChineseCharacterHandler();
        Scanner in = new Scanner(System.in);
        while (in.hasNext()) {
            for (String string : in.next().trim().split(" ")) {
                System.out.println(handler.splitAll(string));
            }
        }
    }
}

//常用的電腦輸入裝置
//常用的電腦輸入裝置
//常用的電腦輸入裝置
