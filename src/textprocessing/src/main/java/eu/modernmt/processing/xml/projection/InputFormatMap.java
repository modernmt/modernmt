package eu.modernmt.processing.xml.projection;

import eu.modernmt.model.Tag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class InputFormatMap {
    static Tag[] transform(Tag[] tags) { return tags; };
    protected static boolean isCompliant(Tag[] tags) { return true; }
}

class InputFormatMapFactory {
    public static InputFormatMap build(Tag[] tags) {
        if (XLiffInputFormatMap.isCompliant(tags)) {
            return new XLiffInputFormatMap();
        } else if (HtmlInputFormatMap.isCompliant(tags)) {
            return new HtmlInputFormatMap();
        } else {
            return new InputFormatMap();
        }
    }
}

class HtmlInputFormatMap extends InputFormatMap {
    private static final Set<String> emptyHtmlTags = new HashSet<>(Arrays.asList("br"));
    private static final Set<String> htmlTags = new HashSet<>(Arrays.asList("br", "href"));
    private static final float acceptanceRate = 0.5f;

    protected static Tag[] transform(Tag[] tags) {
        Tag[] mappedTags = new Tag[tags.length];
        for (int t = 0 ; t < tags.length ; t++){
            Tag tag = tags[t];
            if ( (tag.getType() == Tag.Type.OPENING_TAG) && emptyHtmlTags.contains(tag.getName()) ) {
                mappedTags[t] = new Tag(tag.getName(),tag.getText(),tag.hasLeftSpace(),tag.getRightSpace(),tag.getPosition(), Tag.Type.EMPTY_TAG, tag.isDTD());
            } else {
                mappedTags[t] = Tag.fromTag(tag);
            }
        }
        return mappedTags;
    }

    protected static boolean isCompliant(Tag[] tags) {
        if (tags.length == 0){
             return true;
        }

        int occurrences = (int) Arrays.stream(tags).filter(tag -> htmlTags.contains(tag.getName())).count();

        return ((float) occurrences / tags.length) > acceptanceRate;
    }
}


class XLiffInputFormatMap extends InputFormatMap {

    private static final Set<String> xliffTags = new HashSet<>(Arrays.asList("ex","bx","bpt","ept")); //TODO: list all xliff-compliant tags

    protected static Tag[] transform(Tag[] tags) {
        Tag[] mappedTags = new Tag[tags.length];
        //TODO: to implement the XLIFF map

        return InputFormatMap.transform(tags);
    }

    protected static boolean isCompliant(Tag[] tags) {
        if (tags.length == 0){
            return true;
        }

        int occurrences = (int) Arrays.stream(tags).filter(tag -> xliffTags.contains(tag.getName())).count();

        return occurrences == tags.length;
    }
}
