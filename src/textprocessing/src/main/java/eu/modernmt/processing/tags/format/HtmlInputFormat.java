package eu.modernmt.processing.tags.format;

import eu.modernmt.model.Tag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class HtmlInputFormat implements InputFormat {

    private static final float ACCEPTANCE_RATE = 0.5f;
    private static final Set<String> EMPTY_TAGS = Collections.singleton("br");
    private static final Set<String> LEGAL_TAGS = new HashSet<>(
            Arrays.asList("a", "abbr", "address", "area", "article", "aside", "audio", "b", "base", "bdi",
                    "bdo", "blockquote", "body", "br", "button", "canvas", "caption", "cite", "code", "col", "colgroup",
                    "data", "datalist", "dd", "del", "details", "dfn", "dialog", "div", "dl", "dt", "em", "embed",
                    "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "head",
                    "header", "hgroup", "hr", "html", "i", "iframe", "img", "input", "ins", "kbd", "label", "legend", "li",
                    "link", "main", "map", "mark", "meta", "meter", "nav", "noframes", "noscript", "object", "ol",
                    "optgroup", "option", "output", "p", "param", "picture", "pre", "progress", "q", "rp", "rt", "rtc",
                    "ruby", "s", "samp", "script", "section", "select", "slot", "small", "source", "span", "strong",
                    "style", "sub", "summary", "sup", "table", "tbody", "td", "template", "textarea", "tfoot", "th",
                    "thead", "time", "title", "tr", "track", "u", "ul", "var", "video", "wbr"));

    public static boolean isCompliant(Tag[] tags) {
        int occurrences = (int) Arrays.stream(tags).filter(tag -> LEGAL_TAGS.contains(tag.getName())).count();
        return ((float) occurrences / tags.length) >= ACCEPTANCE_RATE;
    }

    @Override
    public void transform(Tag[] tags) {
        for (Tag tag : tags) {
            if (EMPTY_TAGS.contains(tag.getName())) {
                tag.setType(Tag.Type.EMPTY_TAG);
            }
        }
    }

}
