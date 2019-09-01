package eu.modernmt.processing.xml.format;

import eu.modernmt.model.Tag;

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
        if (XliffInputFormat.isCompliant(tags)) {
            return new XliffInputFormat();
        } else if (HtmlInputFormat.isCompliant(tags)) {
            return new HtmlInputFormat();
        } else {
            return new NoopInputFormat();
        }
    }

    void transform(Tag[] tags);

}
