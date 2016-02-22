package eu.modernmt.processing.util;

import eu.modernmt.model.Sentence;
import eu.modernmt.processing.framework.TextProcessor;

/**
 * Created by davide on 27/01/16.
 */
public class SentenceOutputter implements TextProcessor<Sentence, String> {

    private final boolean outputTags;

    public SentenceOutputter(boolean outputTags) {
        this.outputTags = outputTags;
    }

    @Override
    public String call(Sentence sentence) {
        return outputTags ? sentence.toString() : sentence.getStrippedString();
    }

    @Override
    public void close() {
    }

}