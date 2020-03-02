package eu.modernmt.model;

import eu.modernmt.processing.ProcessingException;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SentenceTest {

    @Test
    public void testCommonSentence() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", null," "),
                new Word("world", " ",null),
                new Word("!", null,null),
        });

        assertEquals("Hello world!", sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
    }

    @Test
    public void testInitialTagWithSpace() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", " "," ", false, true),
                new Word("world", " ",null, true, false),
                new Word("!", null,null, false, false),
        }, new Tag[]{
                XMLTag.fromText("<a>", null, " ", 0)
        });

        assertEquals("<a> Hello world!", sentence.toString(true, false));
        assertEquals("Hello world!", sentence.toString(false, false));
    }

    @Test
    public void testStrippedSentenceWithSpaceAfterTag() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", null,null, false, true),
                new Word("world", " ", null, true, false),
        }, new Tag[]{
                XMLTag.fromText("<a>", null, " ", 1)
        });

        assertEquals("Hello<a> world", sentence.toString(true, false));
        assertEquals("Hello world", sentence.toString(false, false));
    }

    @Test
    public void testStrippedSentenceWithSpacesBetweenTags() {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", null, null, false, true),
                new Word("world", null, null, true, false),
        }, new Tag[]{
                XMLTag.fromText("<a>", null, " ", 1),
                XMLTag.fromText("<b>", " ", null, 1)
        });

        assertEquals("Hello<a> <b>world", sentence.toString(true, false));
        assertEquals("Hello world", sentence.toString(false, false));
    }

    @Test
    @Ignore
    public void testStrippedSentenceWithoutSpacesBetweenTags() throws ProcessingException {
        Sentence sentence = new Sentence(new Word[]{
                new Word("Hello", null, null, false, true),
                new Word("world", null, null, true, false),
        }, new Tag[]{
                XMLTag.fromText("<a>", null, null, 1),
                XMLTag.fromText("<b>", null, null, 1)
        });

        assertEquals("Hello<a><b>world", sentence.toString(true, false));
        assertEquals("Hello world", sentence.toString(false, false));
    }

}
