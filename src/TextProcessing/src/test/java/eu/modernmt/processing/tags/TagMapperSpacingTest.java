package eu.modernmt.processing.tags;

import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.xml.XMLTagMapper;
import org.junit.Test;

import static eu.modernmt.processing.Assertions.assertCoherentSpacing;
import static org.junit.Assert.assertEquals;

public class TagMapperSpacingTest {

    private static Translation translation(Token[] tokens, Tag[] tags) {
        return new Translation(tokens, tags, null, null);
    }

    @Test
    public void testTranslationWithTags() {
        Translation translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<a>", true, false, 1),
                Tag.fromText("</a>", false, false, 2),
        });
        XMLTagMapper.restoreTagSpacing(translation);

        assertEquals("Hello world!", translation.getStrippedString(false));
        assertEquals("Hello <a>world</a>!", translation.toString());
        assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithDiscordantTagSpacing_FalseTrue() {
        Translation translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<a>", true, false, 1),
                Tag.fromText("</a>", false, true, 2),
        });
        XMLTagMapper.restoreTagSpacing(translation);

        assertEquals("Hello world!", translation.getStrippedString(false));
        assertEquals("Hello <a>world</a>!", translation.toString());
        assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithDiscordantTagSpacing_TrueFalse() {
        Translation translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", true),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<a>", true, false, 1),
                Tag.fromText("</a>", false, false, 2),
        });
        XMLTagMapper.restoreTagSpacing(translation);

        assertEquals("Hello world !", translation.getStrippedString(false));
        assertEquals("Hello <a>world</a> !", translation.toString());
        assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedTagList() {
        Translation translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<a>", true, true, 1),
                Tag.fromText("<b>", true, true, 1),
                Tag.fromText("</a>", true, true, 1),
                Tag.fromText("</b>", true, true, 1),
        });
        XMLTagMapper.restoreTagSpacing(translation);

        assertEquals("Hello world!", translation.getStrippedString(false));
        assertEquals("Hello <a><b></a></b>world!", translation.toString());
        assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithTagListSpaceInMiddle() {
        Translation translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("</a>", true, true, 1),
                Tag.fromText("<b>", true, true, 1),
                Tag.fromText("</b>", true, true, 1),
        });
        XMLTagMapper.restoreTagSpacing(translation);

        assertEquals("Hello world!", translation.getStrippedString(false));
        assertEquals("Hello</a> <b></b>world!", translation.toString());
        assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithTagInUnbreakableTokenList() {
        Translation translation = translation(new Token[]{
                new Token("That", false),
                new Token("'s", true),
                new Token("it", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<b>", true, true, 1),
                Tag.fromText("</b>", true, true, 2),
        });
        XMLTagMapper.restoreTagSpacing(translation);

        assertEquals("That's it!", translation.getStrippedString(false));
        assertEquals("That<b>'s</b> it!", translation.toString());
        assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag() {
        Translation translation = translation(new Token[]{
                new Token("This", true),
                new Token("is", true),
                new Token("XML", true),
                new Token("comment", false),
        }, new Tag[]{
                Tag.fromText("<!--", true, true, 2),
                Tag.fromText("-->", true, false, 4),
        });
        XMLTagMapper.restoreTagSpacing(translation);

        assertEquals("This is XML comment", translation.getStrippedString(false));
        assertEquals("This is <!-- XML comment -->", translation.toString());
        assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag_NoLeadingSpace() {
        Translation translation = translation(new Token[]{
                new Token("This", true),
                new Token("is", true),
                new Token("XML", true),
                new Token("comment", false),
        }, new Tag[]{
                Tag.fromText("<!--", false, true, 2),
                Tag.fromText("-->", true, false, 4),
        });
        XMLTagMapper.restoreTagSpacing(translation);

        assertEquals("This is XML comment", translation.getStrippedString(false));
        assertEquals("This is<!-- XML comment -->", translation.toString());
        assertCoherentSpacing(translation);
    }

    @Test
    public void testTranslationWithSpacedCommentTag_TrailingSpace() {
        Translation translation = translation(new Token[]{
                new Token("This", true),
                new Token("is", true),
                new Token("XML", true),
                new Token("comment", false),
        }, new Tag[]{
                Tag.fromText("<!--", false, true, 2),
                Tag.fromText("-->", true, true, 4),
        });
        XMLTagMapper.restoreTagSpacing(translation);

        assertEquals("This is XML comment", translation.getStrippedString(false));
        assertEquals("This is<!-- XML comment -->", translation.toString());
        assertCoherentSpacing(translation);
    }

}
