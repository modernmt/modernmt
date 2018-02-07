package eu.modernmt.processing.numbers;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.numbers.internal.NumericPlaceholder;
import eu.modernmt.processing.numbers.internal.NumericSequence;
import eu.modernmt.processing.numbers.internal.Phrase;

import java.util.List;
import java.util.Map;

/**
 * Created by davide on 08/04/16.
 */
public class NumericWordPostprocessor extends TextProcessor<Translation, Translation> {

    public NumericWordPostprocessor(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        NumericSequence nSentence = NumericSequence.build(translation.getSource());
        NumericSequence nTranslation = NumericSequence.build(translation);

        tryWithPhrases(translation, nSentence, nTranslation);
        tryWithPositionAndPlaceholderDigits(translation, nSentence, nTranslation);
        tryWithPlaceholderPosition(translation, nSentence, nTranslation);

        for (NumericPlaceholder placeholder : nTranslation)
            placeholder.obfuscate();

        return translation;
    }

    private static void tryWithPhrases(Translation translation, NumericSequence sourceSequence, NumericSequence targetSequence) {
        if (targetSequence.isEmpty())
            return;

        List<Phrase> phrases = Phrase.extract(translation, sourceSequence, targetSequence);

        for (Phrase phrase : phrases) {
            int sourceDigitsCount = phrase.countSourceDigits();
            int targetDigitsCount = phrase.countTargetDigits();

            if (sourceDigitsCount == targetDigitsCount)
                phrase.copySourceToTargetDigits();
            else
                phrase.copySourceToTargetWords();
        }
    }

    private static void tryWithPositionAndPlaceholderDigits(Translation translation, NumericSequence sourceSequence, NumericSequence targetSequence) {
        // TODO
    }

    private static void tryWithPlaceholderPosition(Translation translation, NumericSequence sourceSequence, NumericSequence targetSequence) {
        // TODO
    }

}
