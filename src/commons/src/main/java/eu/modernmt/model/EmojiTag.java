package eu.modernmt.model;

import java.util.regex.Pattern;

public class EmojiTag extends Tag {

//    Emojy (and other icons) list taken from:
//    http:/www.fileformat.info/info/charset/UTF-32BE/list.htm
//    https://www.baeldung.com/java-string-remove-emojis}/

    public static final Pattern TagRegex = Pattern.compile("[\\x{00002700}-\\x{000027ff}\\x{00002900}-\\x{0000297f}\\x{00002b00}-\\x{00002bff}\\x{0001f000}-\\x{0001f265}\\x{0001f300}-\\x{0001f64f}\\x{0001f680}-\\x{0001f6ff}\\x{0001f7e0}-\\x{0001f7eb}\\x{0001f800}-\\x{0001f8ff}\\x{0001f900}-\\x{0001f9ff}\\x{0001fa70}-\\x{0001fa95}]+");

    private static final String NAME = "EmojiTag";

    public static EmojiTag fromText(String text) { return fromText(text, false, null, -1); }
    
    public static EmojiTag fromText(String text, boolean leftSpace, String rightSpace, int position) {
        return new EmojiTag(NAME, text, leftSpace, rightSpace, position, Type.EMPTY_TAG);
    }

    private EmojiTag(String name, String text, boolean leftSpace, String rightSpace, int position, Tag.Type type) {
        super(name, text, leftSpace, rightSpace, position, type);
    }

    @Override
    public String toString() {
        return text == null ? placeholder : text;
    }
}
