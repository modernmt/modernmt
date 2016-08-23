package eu.modernmt.processing.vocabulary;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.processing.framework.LanguageNotSupportedException;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.vocabulary.Vocabulary;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 23/08/16.
 */
public class VocabularyLookup extends TextProcessor<Sentence, Sentence> {

    public VocabularyLookup(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Sentence call(Sentence sentence, Map<String, Object> metadata) throws ProcessingException {
        Vocabulary vocabulary = (Vocabulary) metadata.get(TextProcessor.KEY_VOCABULARY);

        if (vocabulary != null)
            lookup(vocabulary, sentence);

        return sentence;
    }

    private void lookup(Vocabulary vocabulary, Sentence sentence) {
        Word[] words = sentence.getWords();

        if (words == null || words.length == 0)
            return;

        String[] line = new String[words.length];
        for (int i = 0; i < line.length; i++)
            line[i] = words[i].getPlaceholder();

        int[] ids = vocabulary.lookupLine(line, false);

        for (int i = 0; i < words.length; i++)
            words[i].setId(ids[i]);
    }

}
