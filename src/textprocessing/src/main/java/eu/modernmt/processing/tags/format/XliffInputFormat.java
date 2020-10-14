package eu.modernmt.processing.tags.format;

import eu.modernmt.model.Tag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class XliffInputFormat implements InputFormat {

//    OPENING_TAGS and CLOSING_TAGS cannot overlap
    private static final Set<String> OPENING_TAGS = Collections.singleton("bx");
    private static final Set<String> CLOSING_TAGS = Collections.singleton("ex");
    private static final Set<String> LEGAL_TAGS = new HashSet<>(Arrays.asList("g", "x", "ex", "bx", "bpt", "ept", "ph", "it", "mrk"));

    static boolean isCompliant(Tag[] tags) {
        int occurrences = (int) Arrays.stream(tags).filter(tag -> LEGAL_TAGS.contains(tag.getName())).count();
        return occurrences == tags.length;
    }

    @Override
    public void transform(Tag[] tags) {
        for (Tag tag : tags) {
            if (OPENING_TAGS.contains(tag.getName())) {
                tag.setType(Tag.Type.SEPARATOR_TAG);
            } else if (CLOSING_TAGS.contains(tag.getName())) {
                tag.setType(Tag.Type.SEPARATOR_TAG);
            }
        }
    }
    
}
