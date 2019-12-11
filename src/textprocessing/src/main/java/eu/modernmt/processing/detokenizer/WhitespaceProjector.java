package eu.modernmt.processing.detokenizer;

import eu.modernmt.lang.Language;
import eu.modernmt.model.*;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.detokenizer.jflex.JFlexDetokenizer;
import eu.modernmt.processing.detokenizer.jflex.JFlexSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.SpacesAnnotatedString;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by davide on 24/03/17.
 */
public class WhitespaceProjector extends TextProcessor<Translation, Translation> {

    private final JFlexSpaceAnnotator annotator;

    public WhitespaceProjector(Language sourceLanguage, Language targetLanguage) {
        super(sourceLanguage, targetLanguage);
        this.annotator = JFlexDetokenizer.newAnnotator(sourceLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        if (!translation.hasAlignment())
            return translation;

        Sentence source = applyAnnotator(translation.getSource());

        Word[] sourceWords = source.getWords();
        Word[] targetWords = translation.getWords();

        HashSet<AlignmentPoint> alignment = AlignmentPoint.parse(translation.getWordAlignment());

        AlignmentPoint probe = new AlignmentPoint();
        for (AlignmentPoint point : alignment) {
            probe.source = point.source + 1;
            probe.target = point.target + 1;

            if (!alignment.contains(probe))
                continue;

            Word pointSourceWord = sourceWords[point.source];
            Word pointTargetWord = targetWords[point.target];
            Word probeSourceWord = sourceWords[probe.source];
            Word probeTargetWord = targetWords[probe.target];

            boolean project = (pointSourceWord.isRightSpaceRequired() && pointTargetWord.hasRightSpace()) ||
                    (pointSourceWord.hasRightSpace() && !pointSourceWord.isRightSpaceRequired());

            if (project) {
                pointTargetWord.setVirtualRightSpace(pointSourceWord.isVirtualRightSpace());
                pointTargetWord.setVirtualLeftSpace(pointSourceWord.isVirtualLeftSpace());
                pointTargetWord.setRightSpace(pointSourceWord.getRightSpace());
                probeTargetWord.setLeftSpace(probeSourceWord.getLeftSpace());
            }
        }

        return translation;
    }

    private Sentence applyAnnotator(Sentence sentence) throws ProcessingException {
        SpacesAnnotatedString text = SpacesAnnotatedString.fromSentence(sentence);

        annotator.reset(text.getReader());

        int type;
        while ((type = next(annotator)) != JFlexSpaceAnnotator.YYEOF) {
            annotator.annotate(text, type);
        }

        text.applyLeft(sentence, Word::setLeftSpaceRequired);
        text.applyRight(sentence, Word::setRightSpaceRequired);
        return sentence;
    }

    private static int next(JFlexSpaceAnnotator annotator) throws ProcessingException {
        try {
            return annotator.next();
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }

    private static class AlignmentPoint {

        public int source;
        public int target;

        public static HashSet<AlignmentPoint> parse(Alignment alignment) {
            HashSet<AlignmentPoint> result = new HashSet<>(alignment.size());
            for (int[] point : alignment)
                result.add(new AlignmentPoint(point[0], point[1]));
            return result;
        }

        private AlignmentPoint() {
            this(0, 0);
        }

        private AlignmentPoint(int source, int target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AlignmentPoint that = (AlignmentPoint) o;

            if (source != that.source) return false;
            return target == that.target;
        }

        @Override
        public int hashCode() {
            int result = source;
            result = 31 * result + target;
            return result;
        }
    }

}
