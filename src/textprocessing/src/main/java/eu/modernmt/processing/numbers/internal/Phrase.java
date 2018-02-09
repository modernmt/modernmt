package eu.modernmt.processing.numbers.internal;

import eu.modernmt.model.Alignment;
import eu.modernmt.model.Translation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public class Phrase {

    public static List<Phrase> extract(Translation translation, NumericSequence sourceSequence, NumericSequence targetSequence) {
        if (!translation.hasAlignment())
            return Collections.emptyList();

        Alignment alignment = translation.getWordAlignment();
        int sourceLength = translation.getSource().getWords().length;
        int targetLength = translation.getWords().length;

        ArrayList<Phrase> phrases = new ArrayList<>();
        BitSet coveredInSource = new BitSet(sourceLength);
        BitSet coveredInTarget = new BitSet(targetLength);

        BitSet coveredPerPhraseInSource = new BitSet(sourceLength);
        BitSet coveredPerPhraseInTarget = new BitSet(targetLength);

        for (int i = 0; i < sourceLength; i++) {
            if (coveredInSource.get(i))
                continue;

            coveredPerPhraseInSource.clear();
            coveredPerPhraseInTarget.clear();
            cover(i, true, coveredPerPhraseInSource, coveredPerPhraseInTarget, alignment);

            coveredInSource.or(coveredPerPhraseInSource);
            coveredInTarget.or(coveredPerPhraseInTarget);

            NumericPlaceholder[] source = extractPlaceholders(sourceSequence, coveredPerPhraseInSource);
            NumericPlaceholder[] target = extractPlaceholders(targetSequence, coveredPerPhraseInTarget);

            if (source.length > 0 && target.length > 0)
                phrases.add(new Phrase(sourceSequence, source, targetSequence, target));
        }

        return phrases;
    }

    private static void cover(int word, boolean fromSource, BitSet coveredPerPhraseInSource, BitSet coveredPerPhraseInTarget, Alignment alignment) {
        if (fromSource)
            coveredPerPhraseInSource.set(word);
        else
            coveredPerPhraseInTarget.set(word);

        for (int[] point : alignment) {
            if (fromSource) {
                if (point[0] == word && !coveredPerPhraseInTarget.get(point[1]))
                    cover(point[1], false, coveredPerPhraseInSource, coveredPerPhraseInTarget, alignment);
            } else {
                if (point[1] == word && !coveredPerPhraseInSource.get(point[0]))
                    cover(point[0], true, coveredPerPhraseInSource, coveredPerPhraseInTarget, alignment);
            }
        }
    }

    private static NumericPlaceholder[] extractPlaceholders(NumericSequence sequence, BitSet map) {
        int[] indexes = new int[map.cardinality()];
        int size = 0;

        for (int i = 0; i < map.length(); i++) {
            if (map.get(i) && sequence.hasIndex(i))
                indexes[size++] = i;
        }

        NumericPlaceholder[] result = new NumericPlaceholder[size];
        for (int i = 0; i < size; i++)
            result[i] = sequence.getByIndex(indexes[i]);

        return result;
    }

    private final NumericSequence sourceSequence;
    private final NumericSequence targetSequence;
    private final NumericPlaceholder[] source;
    private final NumericPlaceholder[] target;

    private Phrase(NumericSequence sourceSequence, NumericPlaceholder[] source, NumericSequence targetSequence, NumericPlaceholder[] target) {
        this.sourceSequence = sourceSequence;
        this.targetSequence = targetSequence;
        this.source = source;
        this.target = target;
    }

    public int countSourceDigits() {
        int count = 0;
        for (NumericPlaceholder e : source)
            count += e.getDigits().length;
        return count;
    }

    public int countTargetDigits() {
        int count = 0;
        for (NumericPlaceholder e : target)
            count += e.getDigits().length;
        return count;
    }

    public void copySourceToTargetDigits() {
        char[] digits = NumericUtils.joinDigits(source);
        int i = 0;

        for (NumericPlaceholder e : target) {
            if (i >= digits.length)
                break;

            i += e.setDigits(digits, i);
        }

        // Remove covered placeholders

        for (NumericPlaceholder e : source)
            sourceSequence.remove(e);
        for (NumericPlaceholder e : target)
            targetSequence.remove(e);
    }

    public void copySourceToTargetWords() {
        for (int i = 0; i < source.length && i < target.length; i++) {
            target[i].getWord().setText(source[i].getWord().getText());

            sourceSequence.remove(source[i]);
            targetSequence.remove(target[i]);
        }
    }
}
