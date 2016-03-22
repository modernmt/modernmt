package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.processing.framework.ProcessingException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

public class PreprocessorTest {

    private static Sentence process(String text, boolean tokenize) throws ProcessingException {
        Preprocessor preprocessor = new Preprocessor(Locale.ENGLISH);

        try {
            return preprocessor.process(text, tokenize);
        } finally {
            IOUtils.closeQuietly(preprocessor);
        }
    }

    @Test
    public void testCommonSentence() throws ProcessingException {
        String text = "Hello world!";
        Sentence sentence = process(text, true);

        assertEquals(text, sentence.toString(false));
        assertEquals("Hello world!", sentence.getStrippedString(false));
        assertFalse(sentence.hasTags());

        assertArrayEquals(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, sentence.getWords());
    }

    @Test
    public void testInitialTagWithSpace() throws ProcessingException {
        String text = "<a> Hello world!";
        Sentence sentence = process(text, true);

        assertEquals(text, sentence.toString(false));
        assertEquals("Hello world!", sentence.getStrippedString(false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", false, true, 0)
        }, sentence.getTags());
    }

    @Test
    public void testStrippedSentenceWithSpaceAfterTag() throws ProcessingException {
        String text = "Hello<a> world!";
        Sentence sentence = process(text, true);

        assertEquals(text, sentence.toString(false));
        assertEquals("Hello world!", sentence.getStrippedString(false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Token[]{
                new Token("Hello", false),
                new Token("world", false),
                new Token("!", false),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", false, true, 1)
        }, sentence.getTags());
    }

    @Test
    public void testStrippedSentenceWithSpacesBetweenTags() throws ProcessingException {
        String text = "Hello<a> <b>world!";
        Sentence sentence = process(text, true);

        assertEquals(text, sentence.toString(false));
        assertEquals("Hello world!", sentence.getStrippedString(false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Token[]{
                new Token("Hello", false),
                new Token("world", false),
                new Token("!", false),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", false, true, 1),
                Tag.fromText("<b>", true, false, 1)
        }, sentence.getTags());
    }

}
