package eu.modernmt.processing.tags.format;

import eu.modernmt.model.Tag;

public class NoopInputFormat implements InputFormat {

    @Override
    public void transform(Tag[] tags) {
        // do nothing
    }

}
