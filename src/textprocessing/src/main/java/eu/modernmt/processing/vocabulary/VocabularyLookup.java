package eu.modernmt.processing.vocabulary;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.vocabulary.Vocabulary;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 23/08/16.
 */
public class VocabularyLookup extends TextProcessor<Sentence, Sentence> {

    public static final String KEY_VOCABULARY = "VocabularyLookup.VOCABULARY"; // Default null
    public static final String KEY_VOCABULARY_STORE_WORDS = "VocabularyLookup.VOCABULARY_STORE_WORDS"; // Default false

    public VocabularyLookup(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Sentence call(Sentence sentence, Map<String, Object> metadata) throws ProcessingException {
        Vocabulary vocabulary = (Vocabulary) metadata.get(KEY_VOCABULARY);

        if (vocabulary != null) {
            Boolean storeWords = (Boolean) metadata.get(KEY_VOCABULARY_STORE_WORDS);
            boolean putIfAbsent = storeWords != null && storeWords;

            lookup(vocabulary, sentence, putIfAbsent);
        }

        return sentence;
    }

    private void lookup(Vocabulary vocabulary, Sentence sentence, boolean putIfAbsent) {
        Word[] words = sentence.getWords();

        if (words == null || words.length == 0)
            return;

        String[] line = new String[words.length];
        for (int i = 0; i < line.length; i++)
            line[i] = words[i].getPlaceholder();

        int[] ids = vocabulary.lookupLine(line, putIfAbsent);

        HashMap<String, Integer> reverseVocabulary = new HashMap<>();
        long unseenWordsCounter = 0xFFFFFFFF;

        for (int i = 0; i < words.length; i++) {
            Word word = words[i];
            int id = ids[i];

            if (id == Vocabulary.VOCABULARY_UNKNOWN_WORD) {
                String text = word.toString();

                Integer unseenId = reverseVocabulary.get(text);

                if (unseenId == null) {
                    unseenId = (int) (unseenWordsCounter--);
                    reverseVocabulary.put(text, unseenId);
                }

                word.setId(unseenId);
                word.setOutOfVocabulary(true);
            } else {
                word.setId(id);
            }
        }
    }

}
