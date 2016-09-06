package eu.modernmt.processing.vocabulary;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.vocabulary.Vocabulary;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static eu.modernmt.processing.vocabulary.VocabularyLookup.KEY_VOCABULARY;

/**
 * Created by davide on 23/08/16.
 */
public class VocabularyReverseLookup extends TextProcessor<Translation, Translation> {

    public VocabularyReverseLookup(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        Vocabulary vocabulary = (Vocabulary) metadata.get(KEY_VOCABULARY);

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
        HashMap<Integer, Word> oovs = getOOVMap(translation.getSource());

        for (int i = 0; i < words.length; i++) {
            Word word = words[i];

            if (strings[i] == null) { // OOV
                Word twin = oovs.get(word.getId());

                if (twin == null) {
                    word.setPlaceholder("");
                } else {
                    word.setPlaceholder(twin.getPlaceholder());
                    word.setText(twin.getText());
                }
            } else {
                word.setPlaceholder(strings[i]);
            }
        }
    }

    private HashMap<Integer, Word> getOOVMap(Sentence sentence) {
        HashMap<Integer, Word> oovMap = new HashMap<>(sentence.length());
        for (Word word : sentence.getWords()) {
            if (word.isOutOfVocabulary())
                oovMap.put(word.getId(), word);
        }

        return oovMap;
    }

}
