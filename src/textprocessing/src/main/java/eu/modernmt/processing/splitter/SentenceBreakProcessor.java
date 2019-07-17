package eu.modernmt.processing.splitter;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.util.Map;

public class SentenceBreakProcessor extends TextProcessor<Sentence, Sentence> {

    private static boolean isBreakToken(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(0);

            if (Character.isWhitespace(c))
                continue;

            switch (c) {
                // Western and Korean
                case '?':
                case '!':
                case '.':
                    // Chinese and Japanese
                case '。':
                case '？':
                case '！':
                    // Arabic
                case '؟':
                    continue;
                default:
                    return false;
            }
        }

        return true;
    }

    @Override
    public Sentence call(Sentence sentence, Map<String, Object> metadata) throws ProcessingException {
        Word sentenceBreakCandidate = null;

        for (Word word : sentence.getWords()) {
            if (isBreakToken(word.getPlaceholder())) {
                sentenceBreakCandidate = word;
            } else if (sentenceBreakCandidate != null) {
                char firstCharOfNextWord = word.getPlaceholder().charAt(0);

                if (Character.isAlphabetic(firstCharOfNextWord) &&
                        (Character.isUpperCase(firstCharOfNextWord) || !Character.isLowerCase(firstCharOfNextWord))) {
                    sentenceBreakCandidate.setSentenceBreak(true);
                }
            }
        }

        return sentence;
    }
}
