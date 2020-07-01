package eu.modernmt.processing.detokenizer;

import eu.modernmt.model.AlignmentPoint;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.TextProcessor;

import java.util.HashSet;
import java.util.Map;

/**
 * Created by davide on 24/03/17.
 */
public class WhitespaceProjector extends TextProcessor<Translation, Translation> {

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) {
        if (!translation.hasAlignment())
            return translation;

        Sentence source = translation.getSource();

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
                pointTargetWord.setHiddenRightSpace(pointSourceWord.hasHiddenRightSpace());
                probeTargetWord.setHiddenLeftSpace(probeSourceWord.hasHiddenLeftSpace());
                pointTargetWord.setRightSpace(pointSourceWord.getRightSpace());
                probeTargetWord.setLeftSpace(probeSourceWord.getLeftSpace());
            }
        }

        return translation;
    }

}
