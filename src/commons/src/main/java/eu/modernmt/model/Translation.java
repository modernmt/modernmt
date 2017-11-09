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
    private Alignment wordAlignment;
    private Alignment sentenceAlignment = null;
    private long elapsedTime;
    private List<Translation> nbest;

    public Translation(Word[] words, Sentence source, Alignment wordAlignment) {
        this(words, null, source, wordAlignment);
    }

    public Translation(Word[] words, Tag[] tags, Sentence source, Alignment wordAlignment) {
        super(words, tags);
        this.source = source;
        this.wordAlignment = wordAlignment;
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

    public void setWordAlignment(Alignment wordAlignment) {
        this.wordAlignment = wordAlignment;
        this.sentenceAlignment = null;
    }

    public Alignment getWordAlignment() {
        return wordAlignment;
    }

    public boolean hasAlignment() {
        return wordAlignment != null;
    }

    public Alignment getSentenceAlignment() {
        if (sentenceAlignment == null && wordAlignment != null) {
            if (!source.hasTags() && !this.hasTags()) {
                sentenceAlignment = wordAlignment;
            } else {
                int[] sourceIdxs = new int[wordAlignment.size()];
                System.arraycopy(wordAlignment.getSourceIndexes(), 0, sourceIdxs, 0, sourceIdxs.length);
                int[] targetIdxs = new int[wordAlignment.size()];
                System.arraycopy(wordAlignment.getTargetIndexes(), 0, targetIdxs, 0, targetIdxs.length);

                shiftAlignment(source.getTags(), sourceIdxs);
                shiftAlignment(this.getTags(), targetIdxs);

                sentenceAlignment = new Alignment(sourceIdxs, targetIdxs);
            }
        }

        return sentenceAlignment;
    }

    private static void shiftAlignment(Tag[] tags, int[] indexes) {
        int t = 0;
        for (int i = 0; i < indexes.length; i++) {
            while (t < tags.length && tags[t].getPosition() == i)
                t++;

            indexes[i] += t;
        }
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

        for (int[] point : wordAlignment) {
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
