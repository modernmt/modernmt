package eu.modernmt.processing.xml;

import eu.modernmt.lang.Language;
import eu.modernmt.model.*;
import eu.modernmt.processing.string.SentenceBuilder;
import eu.modernmt.processing.tags.projection.TagProjector;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class XMLTagProjectorSpacingTest {

    private static Translation translation(Word[] tokens, Tag[] tags) {
        return new Translation(tokens, tags, null, null);
    }

    @Test
    public void testTranslationWithTags() {
        Translation translation = translation(new Word[]{
                new Word("Hello", null, " ", false, true),
                new Word("world", " ", " ", true, false),
                new Word("!", " ", null, false, false),
        }, new Tag[]{
                XMLTag.fromText("<a>", " ", null, 1),
                XMLTag.fromText("</a>", null, null, 2),
        });

        translation.fixWordSpacing();
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world!", translation.toString(false, false));
        assertEquals("Hello <a>world</a>!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithDiscordantTagSpacing_FalseTrue() {
        Translation translation = translation(new Word[]{
                new Word("Hello", null, " ", true, true),
                new Word("world", " ", " ", true,false),
                new Word("!", " ", null, false, false),
        }, new Tag[]{
                XMLTag.fromText("<a>", " ", null, 1),
                XMLTag.fromText("</a>", null, null, 2),
        });

        translation.fixWordSpacing();
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world!", translation.toString(false, false));
        assertEquals("Hello <a>world</a>!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithDiscordantTagSpacing_TrueFalse() {
        Translation translation = translation(new Word[]{
                new Word("Hello", null, " ", true, true),
                new Word("world", " "," ",true, false),
                new Word("!", " ", null, false, true),
        }, new Tag[]{
                XMLTag.fromText("<a>", " ", null, 1),
                XMLTag.fromText("</a>", null, null, 2),
        });

        translation.fixWordSpacing();
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world!", translation.toString(false, false));
        assertEquals("Hello <a>world</a>!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedTagList() {
        Translation translation = translation(new Word[]{
                new Word("Hello", null," ", false, true),
                new Word("world", " ", " ", true, false),
                new Word("!", " ", null, false, false),
        }, new Tag[]{
                XMLTag.fromText("<a>", " ", " ", 1),
                XMLTag.fromText("<b>", " ", " ", 1),
                XMLTag.fromText("</a>", " ", " ", 1),
                XMLTag.fromText("</b>", " ", " ", 1),
        });

        translation.fixWordSpacing();
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello <a> <b> </a> </b> world!", translation.toString());
        assertEquals("Hello world!", translation.toString(false, false));
        Assertions.assertCoherentSpacing(translation);
    }


    @Test
    public void testTranslationWithTagListSpaceInMiddle() {
        Translation translation = translation(new Word[]{
                new Word("Hello", null, " ", false, true),
                new Word("world", " ", " ", true, false),
                new Word("!", " ", null, false, false),
        }, new Tag[]{
                XMLTag.fromText("</a>", " ", " ", 1),
                XMLTag.fromText("<b>", " ", " ", 1),
                XMLTag.fromText("</b>", " ", " ", 1),
        });

        translation.fixWordSpacing();
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world!", translation.toString(false, false));
        assertEquals("Hello </a> <b> </b> world!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithTagInUnbreakableTokenList() {
        Translation translation = translation(new Word[]{
                new Word("That", null," ", false, false),
                new Word("'s", " "," ", false, true),
                new Word("it", " ", " ", true, false),
                new Word("!", " ", null, false, false),
        }, new Tag[]{
                XMLTag.fromText("<b>", null, null, 1),
                XMLTag.fromText("</b>", null, " ", 2),
        });

        translation.fixWordSpacing();
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("That's it!", translation.toString(false, false));
        assertEquals("That<b>'s</b> it!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag() {
        Translation translation = translation(new Word[]{
                new Word("This", null, " ", false, true),
                new Word("is", " ", " ", true, true),
                new Word("XML", " ", " ", true, true),
                new Word("comment", " ", null, true, false),
        }, new Tag[]{
                XMLTag.fromText("<!--", " ", " ", 2),
                XMLTag.fromText("-->", " ", null, 4),
        });

        translation.fixWordSpacing();
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("This is XML comment", translation.toString(false, false));
        assertEquals("This is <!-- XML comment -->", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag_NoLeadingSpace() {
        Translation translation = translation(new Word[]{
                new Word("This", null, " ", false, true),
                new Word("is", " ", " ", true, true),
                new Word("XML", " ", " ", true, true),
                new Word("comment", " ", null, true, false),
        }, new Tag[]{
                XMLTag.fromText("<!--", null, " ", 2),
                XMLTag.fromText("-->", " ", null, 4),
        });

        translation.fixWordSpacing();
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("This is XML comment", translation.toString(false, false));
        assertEquals("This is<!-- XML comment -->", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag_TrailingSpace() {
        Translation translation = translation(new Word[]{
                new Word("This", null, " ", false, true),
                new Word("is", " ", " ", true, true),
                new Word("XML", " ", " ", true, true),
                new Word("comment", " ", null, true, false),
        }, new Tag[]{
                XMLTag.fromText("<!--", null, " ", 2),
                XMLTag.fromText("-->", " ", " ", 4),
        });

        translation.fixWordSpacing();
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("This is XML comment", translation.toString(false, false));
        assertEquals("This is<!-- XML comment -->", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

}
