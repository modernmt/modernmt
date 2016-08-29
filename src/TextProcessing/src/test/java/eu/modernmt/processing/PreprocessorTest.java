package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Word;
import eu.modernmt.processing.framework.ProcessingException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

public class PreprocessorTest {

    private static Sentence process(String text) throws ProcessingException {
        Preprocessor preprocessor = new Preprocessor(Locale.ENGLISH);

        try {
            return preprocessor.process(text);
        } finally {
            IOUtils.closeQuietly(preprocessor);
        }
    }

    @Test
    public void testCommonSentence() throws ProcessingException {
        String text = "Hello world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(false));
        assertEquals("Hello world!", sentence.getStrippedString(false));
        assertFalse(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", " "),
                new Word("world", "world", null),
                new Word("!", "!", null),
        }, sentence.getWords());
    }

    @Test
    public void testInitialTagWithSpace() throws ProcessingException {
        String text = "<a> Hello world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(false));
        assertEquals("Hello world!", sentence.getStrippedString(false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", " "),
                new Word("world", "world", null),
                new Word("!", "!", null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", false, " ", 0)
        }, sentence.getTags());
    }

    @Test
    public void testStrippedSentenceWithSpaceAfterTag() throws ProcessingException {
        String text = "Hello<a> world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(false));
        assertEquals("Hello world!", sentence.getStrippedString(false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", null),
                new Word("world", "world", null),
                new Word("!", "!", null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", false, " ", 1)
        }, sentence.getTags());
    }

    @Test
    public void testStrippedSentenceWithSpacesBetweenTags() throws ProcessingException {
        String text = "Hello<a> <b>world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(false));
        assertEquals("Hello world!", sentence.getStrippedString(false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", null),
                new Word("world", "world", null),
                new Word("!", "!", null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", false, " ", 1),
                Tag.fromText("<b>", true, null, 1)
        }, sentence.getTags());
    }

}
