package eu.modernmt.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TranslationTest {

    private static Translation translation(Token[] tokens) {
        return new Translation(tokens, null, null);
    }

    private static Translation translation(Token[] tokens, Tag[] tags) {
        return new Translation(tokens, tags, null, null);
    }

    @Test
    public void testEmptyTranslation() {
        Translation translation = translation(null);

        assertEquals("", translation.getStrippedString(false));
        assertEquals("", translation.toString());
    }

    @Test
    public void testTranslationNoTags() {
        Translation translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        });

        assertEquals("Hello world!", translation.getStrippedString(false));
        assertEquals("Hello world!", translation.toString());
    }

    @Test
    public void testTranslationOnlyTags() {
        Translation translation = translation(null, new Tag[]{
                Tag.fromText("<a>", false, false, 0),
                Tag.fromText("</a>", false, false, 0),
        });

        assertEquals("", translation.getStrippedString(false));
        assertEquals("<a></a>", translation.toString());
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

        assertEquals("Hello world!", translation.getStrippedString(false));
        assertEquals("Hello <a>world</a>!", translation.toString());
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

        assertEquals("Hello world !", translation.getStrippedString(false));
        assertEquals("Hello <a>world</a>!", translation.toString());
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

        assertEquals("Hello world !", translation.getStrippedString(false));
        assertEquals("Hello <a>world</a> !", translation.toString());
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

        assertEquals("Hello world!", translation.getStrippedString(false));
        assertEquals("Hello <a><b></a></b>world!", translation.toString());
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

        assertEquals("Hello world!", translation.getStrippedString(false));
        assertEquals("Hello</a> <b></b>world!", translation.toString());
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

        assertEquals("That 's it!", translation.getStrippedString(false));
        assertEquals("That<b>'s</b> it!", translation.toString());
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

        assertEquals("This is XML comment", translation.getStrippedString(false));
        assertEquals("This is <!-- XML comment -->", translation.toString());
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

        assertEquals("This is XML comment", translation.getStrippedString(false));
        assertEquals("This is<!-- XML comment -->", translation.toString());
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

        assertEquals("This is XML comment", translation.getStrippedString(false));
        assertEquals("This is<!-- XML comment -->", translation.toString());
    }

}
