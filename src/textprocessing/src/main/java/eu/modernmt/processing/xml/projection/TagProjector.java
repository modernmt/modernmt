package eu.modernmt.processing.xml.projection;

import eu.modernmt.model.*;

public class TagProjector {

    public Translation project(Translation translation) {
        //TODO: stub-implementation
        Sentence sentence = translation.getSource();

        Tag[] tags = sentence.getTags();
        Word[] sentenceWords = sentence.getWords();
        Word[] translationWords = translation.getWords();
        Alignment alignment = translation.getWordAlignment();

        translation.setTags(/* new tag array */null);

        return translation;
    }

}
