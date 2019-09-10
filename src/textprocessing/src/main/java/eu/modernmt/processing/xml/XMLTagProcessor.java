package eu.modernmt.processing.xml;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.xml.projection.TagProjector;

import java.util.Map;

/**
 * Created by davide on 22/07/16.
 */
public class XMLTagProcessor extends TextProcessor<Translation, Translation> {

    private final TagProjector projector = new TagProjector();

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) {
        return projector.project(translation);
    }

}
