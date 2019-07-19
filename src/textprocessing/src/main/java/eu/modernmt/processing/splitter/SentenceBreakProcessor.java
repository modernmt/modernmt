package eu.modernmt.processing.splitter;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.util.Map;

public class SentenceBreakProcessor extends TextProcessor<Sentence, Sentence> {

    private static boolean isBreakToken(String text) {
        int len = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(0);

            if (Character.isWhitespace(c))
                continue;

            switch (c) {
                case '?':  // Western and Korean
                case '!':  // Western and Korean
                case '.':  // Western and Korean
                case '。': // Chinese and Japanese
                case '？': // Chinese and Japanese
                case '！': // Chinese and Japanese
                case '؟':  // Arabic
                    if (len == 0) {
                        len++;
                        continue;
                    } else {
                        return false;
                    }
                default:
                    return false;
            }
        }

        return true;
    }

    private static char getFirstLetterOrDigit(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c))
                return c;
        }

        return '\0';
    }

    @Override
    public Sentence call(Sentence sentence, Map<String, Object> metadata) throws ProcessingException {
        Word[] words = sentence.getWords();

        Word sentenceBreakCandidate = null;

        for (Word word : words) {
            if (isBreakToken(word.getPlaceholder())) {
                sentenceBreakCandidate = word;
            } else if (sentenceBreakCandidate != null) {
                char firstCharOfNextWord = getFirstLetterOrDigit(word.getPlaceholder());

                if (firstCharOfNextWord == '\0')
                    continue;

                if (Character.isLetter(firstCharOfNextWord) &&
                        (Character.isUpperCase(firstCharOfNextWord) || !Character.isLowerCase(firstCharOfNextWord))) {
                    sentenceBreakCandidate.setSentenceBreak(true);
                }

                sentenceBreakCandidate = null;
            }
        }

        if (sentenceBreakCandidate != null)
            sentenceBreakCandidate.setSentenceBreak(true);

        return sentence;
    }

}
