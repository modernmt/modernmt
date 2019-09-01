package eu.modernmt.processing.xml.projection;

import eu.modernmt.model.Tag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

abstract class InputFormatMap {

    Tag[] tags;

    enum Type {
        HTML,
        XLIFF,
        AUTO,
    }

    static InputFormatMap build(Tag[] tags, InputFormatMap.Type type) {
        if (type == InputFormatMap.Type.XLIFF) {
            System.out.println("select InputFormatMap: XLIFF");
            return new XliffInputFormatMap(tags);
        } else if (type == InputFormatMap.Type.HTML) {
            System.out.println("select InputFormatMap: HTML");
            return new HtmlInputFormatMap(tags);
        } else {
            if (XliffInputFormatMap.isCompliant(tags)) {
                System.out.println("select InputFormatMap: XLIFF");
                return new XliffInputFormatMap(tags);
            } else if (HtmlInputFormatMap.isCompliant(tags)) {
                System.out.println("select InputFormatMap: HTML");
                return new HtmlInputFormatMap(tags);
            } else {
                System.out.println("select InputFormatMap: AUTO");
                return new DefaultInputFormatMap(tags);
            }
        }
    }

    InputFormatMap(Tag[] tags) {
        this.tags = tags;
    }

    abstract void transform();
}

class DefaultInputFormatMap extends InputFormatMap {
    DefaultInputFormatMap(Tag[] tags) {
        super(tags);
    }

    void transform() {
    }
}

class HtmlInputFormatMap extends InputFormatMap {
    private static Set<String> emptyTags = new HashSet<>(Arrays.asList("br"));
    private static Set<String> legalTags = new HashSet<>(Arrays.asList("h", "p", "span", "div", "br", "a", "ul", "li", "ol", "dl", "dt", "dd", "table", "tr", "td", "th", "img"));
    private static float acceptanceRate = 0.5f;

    HtmlInputFormatMap(Tag[] tags) {
        super(tags);
    }

    static boolean isCompliant(Tag[] tags) {
        if (tags.length == 0) {
            return true;
        }
        if (legalTags != null) {
            int occurrences = (int) stream(tags).filter(tag -> legalTags.contains(tag.getName())).count();
            return ((float) occurrences / tags.length) >= acceptanceRate;
        } else {
            return true;
        }
    }

    void transform() {
        if (emptyTags != null) {
            for (Tag tag : tags) {
                if (emptyTags.contains(tag.getName())) {
                    tag.setType(Tag.Type.EMPTY_TAG);
                }
            }
        }
    }
}


class XliffInputFormatMap extends InputFormatMap {
    private static Set<String> openingTags = new HashSet<>(asList("bx"));
    private static Set<String> closingTags = new HashSet<>(asList("ex"));
    private static Set<String> legalTags = new HashSet<>(asList("g", "x", "ex", "bx", "bpt", "ept", "ph", "it", "mrk"));

    XliffInputFormatMap(Tag[] tags) {
        super(tags);
    }

    static boolean isCompliant(Tag[] tags) {
        if (tags.length == 0) {
            return true;
        }
        if (legalTags != null) {
            int occurrences = (int) stream(tags).filter(tag -> legalTags.contains(tag.getName())).count();
            return occurrences == tags.length;
        } else {
            return true;
        }

    }

    void transform() {
        if (openingTags != null) {
            for (Tag tag : tags) {
                if (openingTags.contains(tag.getName())) {
                    tag.setType(Tag.Type.OPENING_TAG);
                }
            }
        }
        if (closingTags != null) {
            for (Tag tag : tags) {
                if (closingTags.contains(tag.getName())) {
                    tag.setType(Tag.Type.CLOSING_TAG);
                }
            }
        }
    }
}
