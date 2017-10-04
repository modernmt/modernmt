package eu.modernmt.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/**
 * Created by davide on 17/02/16.
 */
public class Translation extends Sentence {

    public static Translation emptyTranslation(Sentence source) {
        return new Translation(new Word[0], source, new Alignment(new int[0], new int[0]));
    }

    protected final Sentence source;
    private Alignment alignment;
    private long elapsedTime;
    private List<Translation> nbest;

    public Translation(Word[] words, Sentence source, Alignment alignment) {
        this(words, null, source, alignment);
    }

    public Translation(Word[] words, Tag[] tags, Sentence source, Alignment alignment) {
        super(words, tags);
        this.source = source;
        this.alignment = alignment;
        this.elapsedTime = 0;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public Sentence getSource() {
        return source;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public boolean hasAlignment() {
        return alignment != null && alignment.size() > 0;
    }

    public List<Translation> getNbest() {
        return nbest;
    }

    public boolean hasNbest() {
        return nbest != null && nbest.size() > 0;
    }

    public void setNbest(List<Translation> nbest) {
        this.nbest = nbest;
    }

    public List<Phrase> getPhrases() {
        if (!hasAlignment())
            return Collections.singletonList(new Phrase(0, this.source.getWords(), words));

        Word[] sourceWords = source.getWords();

        ArrayList<Phrase> phrases = new ArrayList<>();
        BitSet coveredInSource = new BitSet(sourceWords.length);
        BitSet coveredInTarget = new BitSet(words.length);

        BitSet coveredPerPhraseInSource = new BitSet(sourceWords.length);
        BitSet coveredPerPhraseInTarget = new BitSet(words.length);

        for (int i = 0; i < sourceWords.length; i++) {
            if (coveredInSource.get(i))
                continue;

            coveredPerPhraseInSource.clear();
            coveredPerPhraseInTarget.clear();
            cover(i, true, coveredPerPhraseInSource, coveredPerPhraseInTarget);

            coveredInSource.or(coveredPerPhraseInSource);
            coveredInTarget.or(coveredPerPhraseInTarget);

            phrases.add(new Phrase(i, extractWords(sourceWords, coveredPerPhraseInSource), extractWords(words, coveredPerPhraseInTarget)));
        }

        for (int i = 0; i < sourceWords.length; i++) {
            if (!coveredInSource.get(i))
                phrases.add(new Phrase(i, new Word[]{sourceWords[i]}, new Word[]{}));
        }

        for (int i = 0; i < words.length; i++) {
            if (!coveredInTarget.get(i))
                phrases.add(new Phrase(-1, new Word[]{}, new Word[]{words[i]}));
        }

        return phrases;
    }

    private void cover(int word, boolean fromSource, BitSet coveredPerPhraseInSource, BitSet coveredPerPhraseInTarget) {
        if (fromSource)
            coveredPerPhraseInSource.set(word);
        else
            coveredPerPhraseInTarget.set(word);

        for (int[] point : alignment) {
            if (fromSource) {
                if (point[0] == word && !coveredPerPhraseInTarget.get(point[1]))
                    cover(point[1], false, coveredPerPhraseInSource, coveredPerPhraseInTarget);
            } else {
                if (point[1] == word && !coveredPerPhraseInSource.get(point[0]))
                    cover(point[0], true, coveredPerPhraseInSource, coveredPerPhraseInTarget);
            }
        }
    }

    private Word[] extractWords(Word[] words, BitSet map) {
        Word[] result = new Word[map.cardinality()];
        int index = 0;

        for (int i = 0; i < words.length; i++) {
            if (map.get(i))
                result[index++] = words[i];
        }

        return result;
    }

}
