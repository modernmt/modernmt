package eu.modernmt.model.xmessage;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by davide on 07/04/16.
 */
public class XMessage {

    private static final int SEG_RAW = 0;
    private static final int SEG_INDEX = 1;
    private static final int SEG_TYPE = 2;
    private static final int SEG_MODIFIER = 3; // modifier or subformat

    // Indices for type keywords
    private static final int TYPE_NULL = 0;
    private static final int TYPE_NUMBER = 1;
    private static final int TYPE_DATE = 2;
    private static final int TYPE_TIME = 3;
    private static final int TYPE_CHOICE = 4;
    private static final int TYPE_NAME = 5;
    private static final int TYPE_TEXT = 6;
    private static final int TYPE_BOOLEAN = 7;
    private static final int TYPE_POSSESSIVE = 8;
    private static final int TYPE_LIST = 9;

    private static final String[] TYPE_KEYWORDS = {
            "number",
            "date",
            "time",
            "choice",
            "name",
            "text",
            "boolean",
            "possessive",
            "list"
    };

    private final String pattern;
    private final XFormat[] formats;

    public static XMessage parse(String text) throws XMessageFormatException {
        char[] pattern = text.toCharArray();

        int formatId = 0;
        ArrayList<XFormat> formats = new ArrayList<>();

        StringBuilder[] segments = new StringBuilder[4];
        segments[SEG_RAW] = new StringBuilder();

        int part = SEG_RAW;
        boolean inQuote = false;
        int braceStack = 0;

        for (int i = 0; i < pattern.length; ++i) {
            char ch = pattern[i];
            if (part == SEG_RAW) {
                if (ch == '\'') {
                    segments[SEG_RAW].append('\'');

                    if (i + 1 < pattern.length && pattern[i + 1] == '\'') {
                        segments[SEG_RAW].append('\'');
                        ++i;
                    } else {
                        inQuote = !inQuote;
                    }
                } else if (ch == '{' && !inQuote) {
                    part = SEG_INDEX;
                    if (segments[SEG_INDEX] == null) {
                        segments[SEG_INDEX] = new StringBuilder();
                    }
                } else {
                    segments[part].append(ch);
                }
            } else {
                if (inQuote) {              // just copy quotes in parts
                    segments[part].append(ch);
                    if (ch == '\'') {
                        inQuote = false;
                    }
                } else {
                    switch (ch) {
                        case ',':
                            if (part < SEG_MODIFIER) {
                                if (segments[++part] == null) {
                                    segments[part] = new StringBuilder();
                                }
                            } else {
                                segments[part].append(ch);
                            }
                            break;
                        case '{':
                            ++braceStack;
                            segments[part].append(ch);
                            break;
                        case '}':
                            if (braceStack == 0) {
                                part = SEG_RAW;

                                XFormat format = makeFormat(formatId++, segments[SEG_INDEX], segments[SEG_TYPE], segments[SEG_MODIFIER]);
                                formats.add(format);

                                segments[0].append(format.getPlaceholder());

                                // throw away other segments
                                segments[SEG_INDEX] = null;
                                segments[SEG_TYPE] = null;
                                segments[SEG_MODIFIER] = null;
                            } else {
                                --braceStack;
                                segments[part].append(ch);
                            }
                            break;
                        case ' ':
                            // Skip any leading space chars for SEG_TYPE.
                            if (part != SEG_TYPE || segments[SEG_TYPE].length() > 0) {
                                segments[part].append(ch);
                            }
                            break;
                        case '\'':
                            inQuote = true;
                            // fall through, so we keep quotes in other parts
                        default:
                            segments[part].append(ch);
                            break;
                    }
                }
            }
        }
        if (braceStack == 0 && part != 0) {
            throw new XMessageFormatException("Unmatched braces in the pattern.");
        }

        return new XMessage(segments[0].toString(), formats.isEmpty() ? null : formats.toArray(new XFormat[formats.size()]));
    }

    private static XFormat makeFormat(int id, CharSequence _index, CharSequence _type, CharSequence _modifier) throws XMessageFormatException {
        String index = _index == null || _index.length() == 0 ? null : _index.toString();
        String type = _type == null || _type.length() == 0 ? null : _type.toString();
        String modifier = _modifier == null || _modifier.length() == 0 ? null : _modifier.toString();

        switch (findKeyword(type, TYPE_KEYWORDS)) {
            case TYPE_NULL:
            case TYPE_NUMBER:
            case TYPE_DATE:
            case TYPE_TIME:
            case TYPE_NAME:
            case TYPE_TEXT:
            case TYPE_POSSESSIVE:
            case TYPE_LIST:
                return new XFormat(id, index, type, modifier);
            case TYPE_BOOLEAN:
            case TYPE_CHOICE:
                return XChoiceFormat.parse(id, index, type, modifier);
            default:
                throw new XMessageFormatException("unknown format type: " + type);
        }
    }

    private static final int findKeyword(String s, String[] list) {
        if (s == null)
            return 0;

        for (int i = 0; i < list.length; ++i) {
            if (s.equals(list[i]))
                return i + 1;
        }

        // Try trimmed lowercase.
        String ls = s.trim().toLowerCase(Locale.ROOT);
        if (!ls.equals(s)) {
            for (int i = 0; i < list.length; ++i) {
                if (ls.equals(list[i]))
                    return i + 1;
            }
        }
        return -1;
    }

    private XMessage(String pattern, XFormat[] formats) {
        this.pattern = pattern;
        this.formats = formats;
    }

    public XFormat[] getFormats() {
        return formats;
    }

    public boolean hasFormats() {
        return formats != null;
    }

    @Override
    public String toString() {
        return pattern;
    }

}
