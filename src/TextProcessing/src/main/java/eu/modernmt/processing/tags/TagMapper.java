package eu.modernmt.processing.tags;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;

import java.io.IOException;

/**
 * Created by davide on 19/02/16.
 */
public class TagMapper implements TextProcessor<Translation, Void> {

    @Override
    public Void call(Translation translation) throws ProcessingException {
        Sentence source = translation.getSource();
        
        if (source.hasTags() && translation.hasAlignment())
            TagManager.remap(translation.getSource(), translation);

        return null;
    }

    @Override
    public void close() throws IOException {
    }

}
