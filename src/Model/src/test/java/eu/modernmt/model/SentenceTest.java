package eu.modernmt.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SentenceTest {

    @Test
    public void testCommonSentence() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        });

        assertEquals("Hello world!", sentence.toString(false));
        assertEquals("Hello world!", sentence.getStrippedString(false));
    }

    @Test
    public void testInitialTagWithSpace() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[] {
                Tag.fromText("<a>", false, true, 0)
        });

        assertEquals("<a> Hello world!", sentence.toString(false));
        assertEquals("Hello world!", sentence.getStrippedString(false));
    }

    @Test
    public void testStrippedSentenceWithSpaceAfterTag() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("Hello", false),
                new Token("world", false),
        }, new Tag[] {
                Tag.fromText("<a>", false, true, 1)
        });

        assertEquals("Hello<a> world", sentence.toString(false));
        assertEquals("Hello world", sentence.getStrippedString(false));
    }

    @Test
    public void testStrippedSentenceWithSpacesBetweenTags() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("Hello", false),
                new Token("world", false),
        }, new Tag[] {
                Tag.fromText("<a>", false, true, 1),
                Tag.fromText("<b>", true, false, 1)
        });

        assertEquals("Hello<a> <b>world", sentence.toString(false));
        assertEquals("Hello world", sentence.getStrippedString(false));
    }

}
