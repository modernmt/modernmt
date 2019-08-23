package eu.modernmt.processing.xml.projection;

import eu.modernmt.model.Tag;

import java.util.HashSet;
import java.util.Set;

abstract class InputFormatMap {
    protected static Tag[] transform(Tag[] tags) {
        return tags;
    }
    protected static boolean isCompliant() {
        return true;
    }
}

class HtmlInputFormatMap extends InputFormatMap {

    static final Set<String> emptyHtmlTags = new HashSet<String>(){{"br"}};
    static final Set<String> htmlTags = new HashSet<String>(){{"br","html"}};
    static final float accptanceRate = 0.5f;

    public HtmlInputFormatMap() {
        super();
    }

    protected static Tag[] transform(Tag[] tags) {
        Tag[] mappedTags = new Tag[tags.length];
        for (int t = 0 ; t < tags.length ; t++){
            if ( (tags[t].getType() == Tag.Type.OPENING_TAG) && emptyHtmlTags.contains(tags[t].getName()) ) {
                mappedTags[t] = new Tag(tags[t].getName(),tags[t].getText(),tags[t].hasLeftSpace(),tags[t].getRightSpace(),tags[t].getPosition(), Tag.Type.EMPTY_TAG, tags[t].isDTD());
            } else {
                mappedTags[t] = tags[t];
            }
        }
        return mappedTags;
    }

    protected static boolean isCompliant(Tag[] tags) {
        if (tags.length == 0){
             return true;
        }

        int occurrences = 0;
        for (int t = 0 ; t < tags.length ; t++) {
            if (htmlTags.contains(tags[t].getName())) {
                occurrences++;
            }
        }

        if (occurrences / tags.length > accptanceRate) {
            return true;
        } else {
            return false;
        }
    }
}


class XLiffInputFormatMap extends InputFormatMap {

    static final Set<String> emptyHtmlTags = new HashSet<String>(){{"br"}};
    static final Set<String> xliffTags = new HashSet<String>(){{"ex","bx","bpt","ept"}}; //TODO: list all xliff-compliant tags

    public XLiffInputFormatMap() {
        super();
    }

    protected static Tag[] transform(Tag[] tags) {
        Tag[] mappedTags = new Tag[tags.length];
        //TODO: to implement the XLIFF map

        return InputFormatMap.transform(tags);
    }

    protected static boolean isCompliant(Tag[] tags) {
        if (tags.length == 0){
            return true;
        }

        for (int t = 0 ; t < tags.length ; t++) {
            if (!xliffTags.contains(tags[t].getName())) {
                return false;
            }
        }

        return true;
    }
}
