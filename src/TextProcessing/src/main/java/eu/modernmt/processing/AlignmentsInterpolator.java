package eu.modernmt.processing;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;

import java.io.IOException;
import java.util.*;

/**
 * Created by lucamastrostefano on 15/04/16.
 */
public class AlignmentsInterpolator implements TextProcessor<Translation, Translation> {

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        int numberOfSourceTokens = translation.getSource().getWords().length;
        int numberOfTranslationTokens = translation.getWords().length;
        int[][] interpolatedAlignments = interpolateAlignments(translation.getAlignment(), numberOfSourceTokens,
                numberOfTranslationTokens);
        translation.setAlignment(interpolatedAlignments);
        return translation;
    }

    public static int[][] interpolateAlignments(int[][] alignments, int numberOfSourceTokens, int numberOfTargetTokens) {
        alignments = Arrays.copyOf(alignments, alignments.length + 1);
        alignments[alignments.length - 1] = new int[]{numberOfSourceTokens, numberOfTargetTokens};
        ArrayList<int[]> interpolatedAlignments = new ArrayList<>(alignments.length);
        Arrays.sort(alignments, new Comparator<int[]>() {
            @Override
            public int compare(int[] a1, int[] a2) {
                int c = a1[0] - a2[0];
                if (c == 0) {
                    c = a1[1] - a2[1];
                }
                return c;
            }
        });
        BitSet targetCoveredTokens = new BitSet(numberOfTargetTokens);
        for (int[] alignment : alignments) {
            targetCoveredTokens.set(alignment[1]);
        }
        int prevSourceIndex = -1;
        int prevTargetIndex = -1;
        for (int alignmentIndex = 0; alignmentIndex < alignments.length; alignmentIndex++) {
            int[] alignment = alignments[alignmentIndex];
            int sourceIndex = alignment[0];
            int targetIndex = alignment[1];
            int sourceDiff = sourceIndex - prevSourceIndex;
            int targetDiff = targetIndex - prevTargetIndex;
            if (sourceDiff > 1 || Math.abs(targetDiff) > 1) {
                int minTarget = Math.min(prevTargetIndex, targetIndex);
                int maxTarget = Math.max(prevTargetIndex, targetIndex);
                boolean monotoneBlock = true;
                for (int t = minTarget + 1; t < maxTarget; t++) {
                    if (targetCoveredTokens.get(t)) {
                        monotoneBlock = false;
                        break;
                    }
                }
                if (monotoneBlock) {
                    if (sourceDiff == 0) {
                        for (int t = minTarget + 1; t < maxTarget; t++) {
                            interpolatedAlignments.add(new int[]{sourceIndex, t});
                        }
                    } else if (targetDiff == 0) {
                        for (int s = prevSourceIndex + 1; s < sourceIndex; s++) {
                            interpolatedAlignments.add(new int[]{s, targetIndex});
                        }
                    } else {
                        for (int s = prevSourceIndex + 1; s < sourceIndex; s++) {
                            for (int t = minTarget + 1; t < maxTarget; t++) {
                                interpolatedAlignments.add(new int[]{s, t});
                            }
                        }
                    }
                }
            }
            if (sourceIndex < numberOfSourceTokens && targetIndex < numberOfTargetTokens) {
                interpolatedAlignments.add(alignment);
            }
            prevSourceIndex = sourceIndex;
            prevTargetIndex = targetIndex;
        }
        int[][] result = new int[interpolatedAlignments.size()][];
        return interpolatedAlignments.toArray(result);
    }

    @Override
    public void close() throws IOException {

    }
}
