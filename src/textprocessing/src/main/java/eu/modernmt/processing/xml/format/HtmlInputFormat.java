package eu.modernmt.processing.xml.format;

import eu.modernmt.model.Tag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class HtmlInputFormat implements InputFormat {

    private static final float ACCEPTANCE_RATE = 0.5f;
    private static final Set<String> EMPTY_TAGS = Collections.singleton("br");
    private static final Set<String> LEGAL_TAGS = new HashSet<>(Arrays.asList("h", "p", "span", "div", "br", "a", "ul", "li", "ol", "dl", "dt", "dd", "table", "tr", "td", "th", "img"));

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
