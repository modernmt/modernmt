package eu.modernmt.processing.vocabulary;

import eu.modernmt.model.Translation;
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
public class VocabularyReverseLookup extends TextProcessor<Translation, Translation> {

    public VocabularyReverseLookup(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        Vocabulary vocabulary = (Vocabulary) metadata.get(TextProcessor.KEY_VOCABULARY);

        if (vocabulary != null)
            reverseLookup(vocabulary, translation);

        return translation;
    }

    private void reverseLookup(Vocabulary vocabulary, Translation translation) {
        Word[] words = translation.getWords();

        if (words == null || words.length == 0)
            return;

        int[] ids = new int[words.length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = words[i].getId();
        }

        String[] strings = vocabulary.reverseLookupLine(ids);

        for (int i = 0; i < words.length; i++) {
            if (strings[i] != null)
                words[i].setPlaceholder(strings[i]);
        }
    }

}
