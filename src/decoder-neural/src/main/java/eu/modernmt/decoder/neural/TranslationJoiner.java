package eu.modernmt.decoder.neural;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.scheduler.TranslationSplit;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TranslationJoiner {

    public static Translation join(Sentence originalSentence, TranslationSplit[] splits, int alternatives) throws DecoderException {
        if (splits.length == 0) {
            return Translation.emptyTranslation(originalSentence);
        } else if (splits.length == 1) {
            TranslationSplit split = splits[0];
            Translation translation = split.getTranslation();

            Sentence sentence = translation.getSource();

            if (originalSentence != sentence) {
                float confidence = translation.getConfidence();
                translation = new Translation(translation.getWords(), originalSentence, translation.getWordAlignment(), translation.getAlternatives());
                translation.setConfidence(confidence);
            }

            translation.setQueueLength(split.getQueueSize());
            translation.setQueueTime(split.getQueueTime());
            translation.setDecodeTime(split.getTranslationTime());

            return translation;
        } else {
            return doJoin(originalSentence, splits, alternatives);
        }
    }

    private static Translation doJoin(Sentence originalSentence, TranslationSplit[] splits, int alternatives) throws DecoderException {
        int wordCount = wordCount(splits);
        int alignmentCount = alignmentCount(splits);

        WordsJoiner words = new WordsJoiner(wordCount);
        AlignmentJoiner alignment = alignmentCount > 0 ? new AlignmentJoiner(alignmentCount) : null;



        int totalNbest = 1;
        for (TranslationSplit split : splits) {
            if (split.getTranslation().hasAlternatives())
                totalNbest *= (split.getTranslation().getAlternatives().size() + 1);
        }

        List<NbestSplit> nbestList = new ArrayList<>(totalNbest);
        for (int n = 0 ; n < totalNbest; n++)
            nbestList.add(new NbestSplit(splits.length));

        int step = totalNbest;
        for (int splitIdx = 0 ; splitIdx < splits.length ; splitIdx++) {
            Translation translation = splits[splitIdx].getTranslation();

            if (translation.hasAlternatives())
                step /= (translation.getAlternatives().size() + 1);

            int n = 0;
            while (n<totalNbest) {
                for (int k = 0; k < step; k++) {
                    nbestList.get(n).multiplyConfidence(translation.getConfidence());
                    nbestList.get(n).addWordCount(translation.getWords().length);
                    nbestList.get(n).addAlignmentCount(translation.getWordAlignment().size());
                    nbestList.get(n).setAlternative(splitIdx, -1);
                    n++;
                }

                if (translation.hasAlternatives()) {
                    List<Translation> alternativeTranslations = translation.getAlternatives();
                    for (int h = 0; h < alternativeTranslations.size(); h++) {
                        Translation alternativeTranslation = alternativeTranslations.get(h);

                        for (int k = 0; k < step; k++) {
                            nbestList.get(n).multiplyConfidence(alternativeTranslation.getConfidence());
                            nbestList.get(n).addWordCount(alternativeTranslation.getWords().length);
                            nbestList.get(n).addAlignmentCount(alternativeTranslation.getWordAlignment().size());
                            nbestList.get(n).setAlternative(splitIdx, h);
                            n++;
                        }
                    }
                }
            }
        }

        for (NbestSplit nbest : nbestList)
            nbest.setConfidence((float) Math.pow(nbest.getConfidence(), 1.0 / nbest.getSplit()));

        ArrayList<NbestSplit> sortedNbestList  = new ArrayList<>(nbestList);

        sortedNbestList.sort(NbestSplit::compareTo);

        for (TranslationSplit split : splits) {
            Translation translation = split.getTranslation();

            // Target words
            words.append(translation.getWords());

            // Alignment
            if (alignment != null)
                alignment.append(split.sentence, translation);
        }

        Translation translation;
        if (alignment == null)
            translation = new Translation(words.build(), originalSentence, null);
        else
            translation = new Translation(words.build(), originalSentence, alignment.build());
        translation.setConfidence(sortedNbestList.get(0).getConfidence());

        // Alternatives
        List<Translation> alternativeTranslations = new ArrayList<>();
        int n = 1;
        while (n <= alternatives) {
            WordsJoiner alternativeWords = new WordsJoiner(sortedNbestList.get(n).getWordCount());
            AlignmentJoiner alternativeAlignment = alignmentCount > 0 ? new AlignmentJoiner(sortedNbestList.get(n).getAlignmentCount()) : null;
            for (int splitIdx = 0 ; splitIdx < splits.length ; splitIdx++) {
                Translation trans = splits[splitIdx].getTranslation();
                int altIdx = sortedNbestList.get(n).getAlternativeIdx(splitIdx);

                Translation alternativeTranslation;

                if (altIdx == -1) {
                    alternativeTranslation = trans;
                } else {
                    if (trans.hasAlternatives()) {
                        if (altIdx < trans.getAlternatives().size()) {
                            alternativeTranslation = trans.getAlternatives().get(altIdx);
                        } else {
                            throw new DecoderException("wrong alternative index");
                        }
                    } else {
                        throw new DecoderException("wrong alternative index (" + splitIdx+ "");
                    }
                }

                // Target words
                alternativeWords.append(alternativeTranslation.getWords());

                // Alignment
                if (alternativeAlignment != null)
                    alternativeAlignment.append(splits[splitIdx].sentence, alternativeTranslation);
            }

            Translation alternativeTranslation;
            if (alternativeAlignment == null)
                alternativeTranslation = new Translation(alternativeWords.build(), originalSentence, null);
            else
                alternativeTranslation = new Translation(alternativeWords.build(), originalSentence, alternativeAlignment.build());

            alternativeTranslation.setConfidence(sortedNbestList.get(n).getConfidence());
            alternativeTranslations.add(alternativeTranslation);
            n++;
        }
        translation.setAlternatives(alternativeTranslations);

        // Calculate stats
        int queueSize = getQueueSize(splits);
        long realTime = getRealTime(splits);
        long computeTime = getComputeTime(splits);
        double totalDecodingTime = getDecodingTime(splits);

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

        private WordsJoiner(int length) {
            this.words = new Word[length];
            this.index = 0;
        }

        public void append(Word[] piece) {
            System.arraycopy(piece, 0, this.words, index, piece.length);

            //force the right space between the existing words and the appended words; no space if there are not existing words
            if (index != 0) {
                this.words[index - 1].setRightSpace(" ");
                this.words[index - 1].setRightSpaceRequired(true);
                this.words[index].setLeftSpace(" ");
                this.words[index].setLeftSpaceRequired(true);
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

        private AlignmentJoiner(int length) {
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

    private static class NbestSplit implements Comparable<NbestSplit> {
        private final int[] alternativeIdx;
        private final int split;
        private int wordCount;
        private int alignmentCount;
        private float confidence;

        NbestSplit(int split) {
            this(split, 0, 0, 1.0f);
        }

        NbestSplit(int split, int wordCount, int alignmentCount, float confidence) {
            this.split = split;
            alternativeIdx = new int[split];
            this.wordCount = wordCount;
            this.alignmentCount = alignmentCount;
            this.confidence = confidence;
        }

        private int getAlternativeIdx(int idx) {
            assert (idx < split);
            return alternativeIdx[idx];
        }

        public int getSplit() {
            return split;
        }

        private int getWordCount() {
            return wordCount;
        }

        private void addWordCount(int wordCount) {
            this.wordCount += wordCount;
        }

        private int getAlignmentCount() {
            return alignmentCount;
        }

        private void addAlignmentCount(int alignmentCount) {
            this.alignmentCount += alignmentCount;
        }

        private float getConfidence() {
            return confidence;
        }

        private void setConfidence(float confidence) {
            this.confidence = confidence;
        }

        private void multiplyConfidence(float confidence) {
            this.confidence *= confidence;
        }

        private void setAlternative(int idx, int val) {
            alternativeIdx[idx] = val;
        }

        public String toString() {

            StringBuilder builder = new StringBuilder();
            builder.append(confidence).append(" ").append(wordCount);
            for (int i = 0 ; i < split ; i++)
                builder.append(" ").append(alternativeIdx[i]);

            return builder.toString();
        }

        @Override
        public int compareTo(@NotNull NbestSplit o) {
            float diff = confidence - o.getConfidence();
            if (diff > 0) return -1;
            if (diff == 0) return 0;
            return 1;
        }
    }
}
