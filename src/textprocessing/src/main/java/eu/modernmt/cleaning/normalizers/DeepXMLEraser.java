package eu.modernmt.cleaning.normalizers;

import eu.modernmt.cleaning.CorpusNormalizer;
import eu.modernmt.model.XMLTag;
import eu.modernmt.processing.xml.XMLCharacterEntity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;

public class DeepXMLEraser implements CorpusNormalizer {

    private static final HashSet<String> BLACKLIST = new HashSet<>();

    static {
        // HTML tags
        BLACKLIST.addAll(Arrays.asList("a", "abbr", "address", "area", "article", "aside", "audio", "b", "base", "bdi",
                "bdo", "blockquote", "body", "br", "button", "canvas", "caption", "cite", "code", "col", "colgroup",
                "data", "datalist", "dd", "del", "details", "dfn", "dialog", "div", "dl", "dt", "em", "embed",
                "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "head",
                "header", "hgroup", "hr", "html", "i", "iframe", "img", "input", "ins", "kbd", "label", "legend", "li",
                "link", "main", "map", "mark", "meta", "meter", "nav", "noframes", "noscript", "object", "ol",
                "optgroup", "option", "output", "p", "param", "picture", "pre", "progress", "q", "rp", "rt", "rtc",
                "ruby", "s", "samp", "script", "section", "select", "slot", "small", "source", "span", "strong",
                "style", "sub", "summary", "sup", "table", "tbody", "td", "template", "textarea", "tfoot", "th",
                "thead", "time", "title", "tr", "track", "u", "ul", "var", "video", "wbr"));

        // TMX v1.4b - https://www.gala-global.org/tmx-14b
        BLACKLIST.addAll(Arrays.asList("bpt", "ept", "it", "ph", "hi"));

        // XLIFF v1.2 - http://docs.oasis-open.org/xliff/v1.2/os/xliff-core.html
        BLACKLIST.addAll(Arrays.asList("g", "x", "bx", "ex", "bpt", "ept", "ph", "it", "mrk"));
    }

    private static boolean isBlacklisted(XMLTag tag) {
        if (tag.isComment() || tag.isDTD() || !tag.isOpeningTag())
            return true;

        String text = tag.getText();
        if (text.indexOf('"') != -1 || text.indexOf('=') != -1)
            return true;

        String name = tag.getName().toLowerCase();
        return BLACKLIST.contains(name);
    }

    private static String stripXML(String line) {
        char[] chars = null;
        StringBuilder builder = null;

        Matcher m = XMLTag.TagRegex.matcher(line);
        int stringIndex = 0;

        while (m.find()) {
            if (chars == null) {
                chars = line.toCharArray();
                builder = new StringBuilder();
            }

            int mstart = m.start();
            int mend = m.end();

            if (stringIndex < mstart)
                builder.append(chars, stringIndex, mstart - stringIndex);

            XMLTag tag = XMLTag.fromText(m.group());
            if (isBlacklisted(tag))
                builder.append(' ');
            else
                builder.append(tag);

            stringIndex = mend;
        }

        if (builder == null)
            return line;

        if (stringIndex < chars.length)
            builder.append(chars, stringIndex, chars.length - stringIndex);

        return builder.toString();
    }

    @Override
    public String normalize(String line) {
        while (true) {
            String xmlStrip = stripXML(line);
            String unescaped = XMLCharacterEntity.unescapeAll(xmlStrip);

            // if no entities found, then break
            if (unescaped.equals(xmlStrip))
                return xmlStrip;

            line = unescaped;
        }
    }

}
