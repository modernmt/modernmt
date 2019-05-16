package eu.modernmt.processing.splitter;

import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;

public class TranslationJoiner {

    public static Translation join(Sentence originalSentence, Sentence[] sentencePieces, Translation[] translationPieces) {
        int globalWordsSize = 0;
        int globalWordAlignmentSize = 0;

        for (Translation piece : translationPieces) {
            globalWordsSize += piece.getWords().length;

            if (piece.hasAlignment())
                globalWordAlignmentSize += piece.getWordAlignment().size();
        }

        long totalDecodeTime = 0L;
        long totalLookupTime = 0L;

        WordsJoiner words = new WordsJoiner(globalWordsSize);
        AlignmentJoiner alignment = globalWordAlignmentSize > 0 ? new AlignmentJoiner(globalWordAlignmentSize) : null;

        for (int i = 0; i < sentencePieces.length; i++) {
            Translation translationPiece = translationPieces[i];
            Sentence sentencePiece = sentencePieces[i];

            // Times
            totalDecodeTime += translationPiece.getDecodeTime();
            totalLookupTime += translationPiece.getMemoryLookupTime();

            // Target words
            words.append(translationPiece.getWords());

            // Alignment
            if (alignment != null)
                alignment.append(sentencePiece, translationPiece);
        }

        Translation globalTranslation;
        if (alignment == null)
            globalTranslation = new Translation(words.build(), originalSentence, null);
        else
            globalTranslation = new Translation(words.build(), originalSentence, alignment.build());

        globalTranslation.setDecodeTime(totalDecodeTime);
        globalTranslation.setMemoryLookupTime(totalLookupTime);

        return globalTranslation;
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
            if (index != 0){
                this.words[index-1].setRightSpace(" ");
                this.words[index-1].setRightSpaceRequired(true);
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
