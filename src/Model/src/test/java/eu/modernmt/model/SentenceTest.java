package eu.modernmt.model;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class SentenceTest {

    @Test
    public void testEmptySentence() {
        Sentence sentence = new Sentence(null, null);

        assertEquals("", sentence.getStrippedString());
        assertEquals("", sentence.toString());
    }

    @Test
    public void testSentenceNoTags() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        });

        assertEquals("Hello world!", sentence.getStrippedString());
        assertEquals("Hello world!", sentence.toString());
    }

    @Test
    public void testSentenceOnlyTags() {
        Sentence sentence = new Sentence(null, new Tag[] {
                Tag.fromText("<a>", false, false, 0),
                Tag.fromText("</a>", false, false, 0),
        });

        assertEquals("", sentence.getStrippedString());
        assertEquals("<a></a>", sentence.toString());
    }

    @Test
    public void testSentenceWithTags() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[] {
                Tag.fromText("<a>", true, false, 1),
                Tag.fromText("</a>", false, false, 2),
        });

        assertEquals("Hello world!", sentence.getStrippedString());
        assertEquals("Hello <a>world</a>!", sentence.toString());
    }

    @Test
    public void testSentenceWithDiscordantTagSpacing_FalseTrue() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[] {
                Tag.fromText("<a>", true, false, 1),
                Tag.fromText("</a>", false, true, 2),
        });

        assertEquals("Hello world!", sentence.getStrippedString());
        assertEquals("Hello <a>world</a>!", sentence.toString());
    }

    @Test
    public void testSentenceWithDiscordantTagSpacing_TrueFalse() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("Hello", true),
                new Token("world", true),
                new Token("!", false),
        }, new Tag[] {
                Tag.fromText("<a>", true, false, 1),
                Tag.fromText("</a>", false, false, 2),
        });

        assertEquals("Hello world !", sentence.getStrippedString());
        assertEquals("Hello <a>world</a> !", sentence.toString());
    }

    @Test
    public void testSentenceWithSpacedTagList() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[] {
                Tag.fromText("<a>", true, true, 1),
                Tag.fromText("<b>", true, true, 1),
                Tag.fromText("</a>", true, true, 1),
                Tag.fromText("</b>", true, true, 1),
        });

        assertEquals("Hello world!", sentence.getStrippedString());
        assertEquals("Hello <a><b></a></b>world!", sentence.toString());
    }

    @Test
    public void testSentenceWithTagListSpaceInMiddle() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[] {
                Tag.fromText("</a>", true, true, 1),
                Tag.fromText("<b>", true, true, 1),
                Tag.fromText("</b>", true, true, 1),
        });

        assertEquals("Hello world!", sentence.getStrippedString());
        assertEquals("Hello</a> <b></b>world!", sentence.toString());
    }

    @Test
    public void testSentenceWithTagInUnbreakableTokenList() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("That", false),
                new Token("'s", true),
                new Token("it", false),
                new Token("!", false),
        }, new Tag[] {
                Tag.fromText("<b>", true, true, 1),
                Tag.fromText("</b>", true, true, 2),
        });

        assertEquals("That's it!", sentence.getStrippedString());
        assertEquals("That<b>'s</b> it!", sentence.toString());
    }

    @Test
    public void testSentenceWithSpacedCommentTag() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("This", true),
                new Token("is", true),
                new Token("XML", true),
                new Token("comment", false),
        }, new Tag[] {
                Tag.fromText("<!--", true, true, 2),
                Tag.fromText("-->", true, false, 4),
        });

        assertEquals("This is XML comment", sentence.getStrippedString());
        assertEquals("This is <!-- XML comment -->", sentence.toString());
    }

    @Test
    public void testSentenceWithSpacedCommentTag_NoLeadingSpace() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("This", true),
                new Token("is", true),
                new Token("XML", true),
                new Token("comment", false),
        }, new Tag[] {
                Tag.fromText("<!--", false, true, 2),
                Tag.fromText("-->", true, false, 4),
        });

        assertEquals("This is XML comment", sentence.getStrippedString());
        assertEquals("This is<!-- XML comment -->", sentence.toString());
    }

    @Test
    public void testSentenceWithSpacedCommentTag_TrailingSpace() {
        Sentence sentence = new Sentence(new Token[]{
                new Token("This", true),
                new Token("is", true),
                new Token("XML", true),
                new Token("comment", false),
        }, new Tag[] {
                Tag.fromText("<!--", false, true, 2),
                Tag.fromText("-->", true, true, 4),
        });

        assertEquals("This is XML comment", sentence.getStrippedString());
        assertEquals("This is<!-- XML comment -->", sentence.toString());
    }

}
