package eu.modernmt.processing;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Word;
import eu.modernmt.model.XMLTag;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class PreprocessorTest {

    private static final LanguageDirection language = new LanguageDirection(Language.ENGLISH, Language.ITALIAN);

    private static Sentence process(String text) throws ProcessingException {
        Preprocessor preprocessor = null;

        try {
            preprocessor = new Preprocessor();
            return preprocessor.process(language, text);
        } catch (IOException e) {
            throw new ProcessingException(e);
        } finally {
            IOUtils.closeQuietly(preprocessor);
        }
    }

    @Test
    public void testCommonSentence() throws ProcessingException {
        String text = "Hello world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
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

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", " "),
                new Word("world", "world", null),
                new Word("!", "!", null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", false, " ", 0)
        }, sentence.getTags());
    }

    @Test
    public void testStrippedSentenceWithSpaceAfterTag() throws ProcessingException {
        String text = "Hello<a> world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", null),
                new Word("world", "world", null),
                new Word("!", "!", null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", false, " ", 1)
        }, sentence.getTags());
    }

    @Test
    public void testStrippedSentenceWithSpacesBetweenTags() throws ProcessingException {
        String text = "Hello<a> <b>world!";
        Sentence sentence = process(text);

        assertEquals(text, sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
        assertTrue(sentence.hasTags());

        assertArrayEquals(new Word[]{
                new Word("Hello", "Hello", null),
                new Word("world", "world", null),
                new Word("!", "!", null),
        }, sentence.getWords());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", false, " ", 1),
                XMLTag.fromText("<b>", true, null, 1)
        }, sentence.getTags());
    }

}
