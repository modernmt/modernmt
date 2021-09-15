package eu.modernmt.processing.tags.format;

import eu.modernmt.model.Tag;
import eu.modernmt.model.XMLTag;
import java.util.Arrays;
import java.util.stream.Collectors;

public interface InputFormat {

    enum Type {
        HTML, XLIFF, XML
    }

    static InputFormat build(Type type) {
        switch (type) {
            case XML:
                return new NoopInputFormat();
            case HTML:
                return new HtmlInputFormat();
            case XLIFF:
                return new XliffInputFormat();
        }

        return null;
    }

    static InputFormat auto(Tag[] tags) {
        XMLTag[] xmlTags = Arrays.asList(tags).stream().filter(tag -> tag instanceof XMLTag).map(tag -> (XMLTag) tag).collect(Collectors.toList()).toArray(new XMLTag[0]);
        if (XliffInputFormat.isCompliant(xmlTags)) {
            return new XliffInputFormat();
        } else if (HtmlInputFormat.isCompliant(xmlTags)) {
            return new HtmlInputFormat();
        } else {
            return new NoopInputFormat();
        }
    }

    void transform(Tag[] tags);

}
