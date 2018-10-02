package eu.modernmt.model;

import java.util.List;

/**
 * Created by davide on 17/02/16.
 */
public class Translation extends Sentence {

    public static Translation emptyTranslation(Sentence source) {
        return new Translation(new Word[0], source, new Alignment(new int[0], new int[0]));
    }

    public static Translation fromTokens(Sentence source, String[] tokens) {
        Word[] words = new Word[tokens.length];
        for (int i = 0; i < words.length; i++)
            words[i] = new Word(tokens[i], " ");

        return new Translation(words, source, null);
    }

    protected final Sentence source;
    private Alignment wordAlignment;
    private Alignment sentenceAlignment = null;
    private List<Translation> nbest;

    // Statistics
    private long memoryLookupTime;
    private long decodeTime;
    private long queueTime;
    private int queueLength;

    public Translation(Word[] words, Sentence source, Alignment wordAlignment) {
        this(words, null, source, wordAlignment);
    }

    public Translation(Word[] words, Tag[] tags, Sentence source, Alignment wordAlignment) {
        super(words, tags);
        this.source = source;
        this.wordAlignment = wordAlignment;
        this.decodeTime = 0;
        this.queueTime = 0;
        this.queueLength = 0;
    }

    public long getMemoryLookupTime() {
        return memoryLookupTime;
    }

    public void setMemoryLookupTime(long memoryLookupTime) {
        this.memoryLookupTime = memoryLookupTime;
    }

    public long getDecodeTime() {
        return decodeTime;
    }

    public void setDecodeTime(long decodeTime) {
        this.decodeTime = decodeTime;
    }

    public long getQueueTime() {
        return queueTime;
    }

    public void setQueueTime(long queueTime) {
        this.queueTime = queueTime;
    }

    public int getQueueLength() {
        return queueLength;
    }

    public void setQueueLength(int queueLength) {
        this.queueLength = queueLength;
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

                sentenceAlignment = new Alignment(sourceIdxs, targetIdxs, wordAlignment.getScore());
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

}
