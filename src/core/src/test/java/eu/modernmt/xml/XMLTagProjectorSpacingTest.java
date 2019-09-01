package eu.modernmt.xml;

import eu.modernmt.model.Tag;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.xml.projection.TagProjector;
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
                new Word("Hello", " "),
                new Word("world", null),
                new Word("!", null),
        }, new Tag[]{
                Tag.fromText("<a>", true, null, 1),
                Tag.fromText("</a>", false, null, 2),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world !", translation.toString(false, false));
        assertEquals("Hello <a>world</a>!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    @Ignore
    public void testTranslationWithDiscordantTagSpacing_FalseTrue() {
        Translation translation = translation(new Word[]{
                new Word("Hello", " "),
                new Word("world", null),
                new Word("!", null),
        }, new Tag[]{
                Tag.fromText("<a>", true, null, 1),
                Tag.fromText("</a>", false, " ", 2),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world !", translation.toString(false, false));
        assertEquals("Hello <a>world</a>!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithDiscordantTagSpacing_TrueFalse() {
        Translation translation = translation(new Word[]{
                new Word("Hello", " "),
                new Word("world", " "),
                new Word("!", null),
        }, new Tag[]{
                Tag.fromText("<a>", true, null, 1),
                Tag.fromText("</a>", false, null, 2),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world !", translation.toString(false, false));
        assertEquals("Hello <a>world</a> !", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedTagList() {
        Translation translation = translation(new Word[]{
                new Word("Hello", " "),
                new Word("world", null),
                new Word("!", null),
        }, new Tag[]{
                Tag.fromText("<a>", true, " ", 1),
                Tag.fromText("<b>", true, " ", 1),
                Tag.fromText("</a>", true, " ", 1),
                Tag.fromText("</b>", true, " ", 1),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world!", translation.toString(false, false));
        assertEquals("Hello <a> <b> </a> </b> world!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithTagListSpaceInMiddle() {
        Translation translation = translation(new Word[]{
                new Word("Hello", " "),
                new Word("world", null),
                new Word("!", null),
        }, new Tag[]{
                Tag.fromText("</a>", true, " ", 1),
                Tag.fromText("<b>", true, " ", 1),
                Tag.fromText("</b>", true, " ", 1),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("Hello world!", translation.toString(false, false));
        assertEquals("Hello </a> <b> </b> world!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    @Ignore
    public void testTranslationWithTagInUnbreakableTokenList() {
        Translation translation = translation(new Word[]{
                new Word("That", null),
                new Word("'s", " "),
                new Word("it", null),
                new Word("!", null),
        }, new Tag[]{
                Tag.fromText("<b>", true, " ", 1),
                Tag.fromText("</b>", true, " ", 2),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("That 's it!", translation.toString(false, false));
        assertEquals("That<b>&apos;s</b> it!", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag() {
        Translation translation = translation(new Word[]{
                new Word("This", " "),
                new Word("is", " "),
                new Word("XML", " "),
                new Word("comment", null),
        }, new Tag[]{
                Tag.fromText("<!--", true, " ", 2),
                Tag.fromText("-->", true, null, 4),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("This is XML comment", translation.toString(false, false));
        assertEquals("This is <!-- XML comment -->", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag_NoLeadingSpace() {
        Translation translation = translation(new Word[]{
                new Word("This", " "),
                new Word("is", " "),
                new Word("XML", " "),
                new Word("comment", null),
        }, new Tag[]{
                Tag.fromText("<!--", false, " ", 2),
                Tag.fromText("-->", true, null, 4),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("This is XML comment", translation.toString(false, false));
        assertEquals("This is<!-- XML comment -->", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag_TrailingSpace() {
        Translation translation = translation(new Word[]{
                new Word("This", " "),
                new Word("is", " "),
                new Word("XML", " "),
                new Word("comment", null),
        }, new Tag[]{
                Tag.fromText("<!--", false, " ", 2),
                Tag.fromText("-->", true, " ", 4),
        });
        TagProjector.simpleSpaceAnalysis(translation);

        assertEquals("This is XML comment", translation.toString(false, false));
        assertEquals("This is<!-- XML comment -->", translation.toString());
        Assertions.assertCoherentSpacing(translation);
    }

}
