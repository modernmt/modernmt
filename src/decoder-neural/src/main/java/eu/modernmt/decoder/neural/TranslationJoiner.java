package eu.modernmt.decoder.neural;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.scheduler.TranslationSplit;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;

public class TranslationJoiner {

    public static Translation join(Sentence originalSentence, Sentence[] splits, TranslationSplit[] translationSplits) throws DecoderException {
        int wordCount = wordCount(translationSplits);
        int alignmentCount = alignmentCount(translationSplits);

        WordsJoiner words = new WordsJoiner(wordCount);
        AlignmentJoiner alignment = alignmentCount > 0 ? new AlignmentJoiner(alignmentCount) : null;

        for (int i = 0; i < splits.length; i++) {
            Translation translationSplit = translationSplits[i].getTranslation();
            Sentence sentenceSplit = splits[i];

            // Target words
            words.append(translationSplit.getWords());

            // Alignment
            if (alignment != null)
                alignment.append(sentenceSplit, translationSplit);
        }

        Translation translation;
        if (alignment == null)
            translation = new Translation(words.build(), originalSentence, null);
        else
            translation = new Translation(words.build(), originalSentence, alignment.build());

        // Calculate stats
        int queueSize = getQueueSize(translationSplits);
        long realTime = getRealTime(translationSplits);
        long computeTime = getComputeTime(translationSplits);
        double totalDecodingTime = getDecodingTime(translationSplits);

        long decodeTime = Math.round((totalDecodingTime / computeTime) * realTime);
        long queueTime = realTime - decodeTime;

        translation.setQueueLength(queueSize);
        translation.setQueueTime(queueTime);
        translation.setDecodeTime(decodeTime);

        return translation;
    }

    private static int wordCount(TranslationSplit[] splits) throws DecoderException {
        int count = 0;
        for (TranslationSplit split : splits)
            count += split.getTranslation().getWords().length;
        return count;
    }

    private static int alignmentCount(TranslationSplit[] splits) throws DecoderException {
        int count = 0;

        for (TranslationSplit split : splits) {
            Translation translation = split.getTranslation();
            if (translation.hasAlignment())
                count += translation.getWordAlignment().size();
        }

        return count;
    }

    private static long getRealTime(TranslationSplit[] splits) {
        long begin = Long.MAX_VALUE, end = 0;
        for (TranslationSplit split : splits) {
            long value = split.getQueueWaitingBegin();
            if (value < begin)
                begin = value;

            value = split.getTranslationEnd();
            if (value > end)
                end = value;
        }

        return end - begin;
    }

    private static int getQueueSize(TranslationSplit[] splits) {
        int size = 0;
        for (TranslationSplit split : splits) {
            int value = split.getQueueSize();
            if (value > size)
                size = value;
        }
        return size;
    }

    private static long getComputeTime(TranslationSplit[] splits) {
        long total = 0;
        for (TranslationSplit split : splits)
            total += split.getTranslationEnd() - split.getQueueWaitingBegin();
        return total;
    }

    private static long getDecodingTime(TranslationSplit[] splits) {
        long total = 0;
        for (TranslationSplit split : splits)
            total += split.getTranslationEnd() - split.getTranslationBegin();
        return total;
    }

    private static class WordsJoiner {

        private final Word[] words;
        private int index;

        public WordsJoiner(int length) {
            this.words = new Word[length];
            this.index = 0;
        }

        public void append(Word[] piece) {
            System.arraycopy(piece, 0, this.words, index, piece.length);

            //force the right space between the existing words and the appended words; no space if there are not exsting words
            if (index != 0) {
                this.words[index - 1].setRightSpace(" ");
                this.words[index - 1].setRightSpaceRequired(true);
            }

            index += piece.length;
        }

        public Word[] build() {
            return words;
        }
    }

    private static class AlignmentJoiner {

        private final int[] src;
        private final int[] tgt;
        private float score = 0;
        private int scoreNorm = 0;
        private int srcOffset = 0;
        private int tgtOffset = 0;
        private int algOffset = 0;

        public AlignmentJoiner(int length) {
            this.src = new int[length];
            this.tgt = new int[length];
        }

        public void append(Sentence sentence, Translation translation) {
            int srcLength = sentence.getWords().length;
            int tgtLength = translation.getWords().length;

            if (translation.hasAlignment()) {
                Alignment alignment = translation.getWordAlignment();

                score += alignment.getScore() * (srcLength + tgtLength);
                scoreNorm += srcLength + tgtLength;

                int[] _src = alignment.getSourceIndexes();
                int[] _tgt = alignment.getTargetIndexes();

                for (int i = 0; i < _src.length; i++) {
                    src[i + algOffset] = _src[i] + srcOffset;
                    tgt[i + algOffset] = _tgt[i] + tgtOffset;
                }

                algOffset += _src.length;
            }

            srcOffset += srcLength;
            tgtOffset += tgtLength;
        }

        public Alignment build() {
            return new Alignment(src, tgt, score / scoreNorm);
        }
    }
}
