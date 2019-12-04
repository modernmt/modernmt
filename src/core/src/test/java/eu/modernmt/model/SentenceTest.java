package eu.modernmt.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SentenceTest {

    @Test
    public void testCommonSentence() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", " "),
                new Word("world", null),
                new Word("!", null),
        });

        assertEquals("Hello world!", sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
    }

    @Test
    public void testInitialTagWithSpace() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", " "),
                new Word("world", null),
                new Word("!", null),
        }, new Tag[]{
                XMLTag.fromText("<a>", false, " ", 0)
        });

        assertEquals("<a> Hello world!", sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
    }

    @Test
    public void testStrippedSentenceWithSpaceAfterTag() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", null),
                new Word("world", null),
        }, new Tag[]{
                XMLTag.fromText("<a>", false, " ", 1)
        });

        assertEquals("Hello<a> world", sentence.toString(true, false));
        assertEquals("Hello world", sentence.toString(false, false));
    }

    @Test
    public void testStrippedSentenceWithSpacesBetweenTags() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", null),
                new Word("world", null),
        }, new Tag[]{
                XMLTag.fromText("<a>", false, " ", 1),
                XMLTag.fromText("<b>", true, null, 1)
        });

        assertEquals("Hello<a> <b>world", sentence.toString(true, false));
        assertEquals("Hello world", sentence.toString(false, false));
    }

    @Test
    public void testStrippedSentenceWithoutSpacesBetweenTags() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", null),
                new Word("world", null),
        }, new Tag[]{
                XMLTag.fromText("<a>", false, null, 1),
                XMLTag.fromText("<b>", false, null, 1)
        });

        assertEquals("Hello<a><b>world", sentence.toString(true, false));
        assertEquals("Hello world", sentence.toString(false, false));
    }

}
