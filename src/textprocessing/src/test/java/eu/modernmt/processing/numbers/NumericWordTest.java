package eu.modernmt.processing.numbers;

import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.ProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NumericWordTest {

    private final NumericWordPostprocessor postprocessor;

    public NumericWordTest() {
        try {
            postprocessor = new NumericWordPostprocessor();
        } catch (UnsupportedLanguageException e) {
            throw new Error(e);
        }
    }

    private static Translation make(String source, String target, int[][] alignment) {
        String[] sourceTokens = source.split("\\s+");
        String[] targetTokens = target.split("\\s+");

        Word[] sourceWords = new Word[sourceTokens.length];
        for (int i = 0; i < sourceTokens.length; i++)
            sourceWords[i] = new Word(sourceTokens[i], sourceTokens[i].replaceAll("[0-9]", "0"), " ");

        Word[] targetWords = new Word[targetTokens.length];
        for (int i = 0; i < targetTokens.length; i++)
            targetWords[i] = new Word(targetTokens[i].replaceAll("[0-9]", "0"), " ");

        return new Translation(targetWords, new Sentence(sourceWords), Alignment.fromAlignmentPairs(alignment));
    }

    @Test
    public void testCurrency_Matching() throws ProcessingException {
        Translation translation = make("1,76$", "$0.00", new int[][]{{0, 0}});
        postprocessor.call(translation, null);
        assertEquals("$1.76", translation.toString(false, false));
    }

    @Test
    public void testCurrency_NotMatching() throws ProcessingException {
        Translation translation = make("1,76$", "$0.000", new int[][]{{0, 0}});
        postprocessor.call(translation, null);
        assertEquals("1,76$", translation.toString(false, false));
    }

    @Test
    public void testBigNumber_Matching() throws ProcessingException {
        Translation translation = make("147.530,50", "000000.00", new int[][]{{0, 0}});
        postprocessor.call(translation, null);
        assertEquals("147530.50", translation.toString(false, false));
    }

    @Test
    public void testBigNumber_NotMatching() throws ProcessingException {
        Translation translation = make("147.530,50", "00/00/00", new int[][]{{0, 0}});
        postprocessor.call(translation, null);
        assertEquals("147.530,50", translation.toString(false, false));
    }

    @Test
    public void testMultiple_Matching() throws ProcessingException {
        Translation translation = make("147.530", "000 000", new int[][]{{0, 0}, {0, 1}});
        postprocessor.call(translation, null);
        assertEquals("147 530", translation.toString(false, false));
    }

    @Test
    public void testMultiple_NotMatching() throws ProcessingException {
        Translation translation = make("147.530", "000 00", new int[][]{{0, 0}, {0, 1}});
        postprocessor.call(translation, null);
        assertEquals("147.530 ??", translation.toString(false, false));
    }

}
