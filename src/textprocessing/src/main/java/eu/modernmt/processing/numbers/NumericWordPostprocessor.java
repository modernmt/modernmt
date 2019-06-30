package eu.modernmt.processing.numbers;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.numbers.internal.NumericPlaceholder;
import eu.modernmt.processing.numbers.internal.NumericSequence;
import eu.modernmt.processing.numbers.internal.Phrase;

import java.util.*;

/**
 * Created by davide on 08/04/16.
 */
public class NumericWordPostprocessor extends TextProcessor<Translation, Translation> {

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        NumericSequence nSentence = NumericSequence.build(translation.getSource());
        NumericSequence nTranslation = NumericSequence.build(translation);

        tryWithPhrases(translation, nSentence, nTranslation);
        tryWithPositionAndPlaceholderDigits(nSentence, nTranslation);
        tryWithPlaceholderPosition(nSentence, nTranslation);

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

    private static void tryWithPositionAndPlaceholderDigits(NumericSequence sourceSequence, NumericSequence targetSequence) {
        if (targetSequence.isEmpty())
            return;

        HashMap<Integer, LinkedList<NumericPlaceholder>> digits2placeholder = new HashMap<>();
        for (NumericPlaceholder e : sourceSequence) {
            LinkedList<NumericPlaceholder> list = digits2placeholder.computeIfAbsent(e.getDigits().length, (key) -> new LinkedList<>());
            list.add(e);
        }

        Iterator<NumericPlaceholder> it = targetSequence.iterator();
        while (it.hasNext()) {
            NumericPlaceholder target = it.next();

            LinkedList<NumericPlaceholder> list = digits2placeholder.get(target.getDigits().length);
            if (list != null && !list.isEmpty()) {
                NumericPlaceholder source = list.remove(0);
                target.setDigits(source.getDigits(), 0);

                it.remove();
                sourceSequence.remove(source);
            }
        }
    }

    private static void tryWithPlaceholderPosition(NumericSequence sourceSequence, NumericSequence targetSequence) {
        if (targetSequence.isEmpty())
            return;

        Iterator<NumericPlaceholder> sourceIterator = sourceSequence.iterator();
        Iterator<NumericPlaceholder> targetIterator = targetSequence.iterator();

        while (sourceIterator.hasNext() && targetIterator.hasNext()) {
            NumericPlaceholder source = sourceIterator.next();
            NumericPlaceholder target = targetIterator.next();

            target.getWord().setText(source.getWord().getText());

            sourceIterator.remove();
            targetIterator.remove();
        }
    }

}
