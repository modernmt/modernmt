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
        Translation Translation = translation(null);

        assertEquals("", Translation.getStrippedString(false));
        assertEquals("", Translation.toString());
    }

    @Test
    public void testTranslationNoTags() {
        Translation Translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        });

        assertEquals("Hello world!", Translation.getStrippedString(false));
        assertEquals("Hello world!", Translation.toString());
    }

    @Test
    public void testTranslationOnlyTags() {
        Translation Translation = translation(null, new Tag[]{
                Tag.fromText("<a>", false, false, 0),
                Tag.fromText("</a>", false, false, 0),
        });

        assertEquals("", Translation.getStrippedString(false));
        assertEquals("<a></a>", Translation.toString());
    }

    @Test
    public void testTranslationWithTags() {
        Translation Translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<a>", true, false, 1),
                Tag.fromText("</a>", false, false, 2),
        });

        assertEquals("Hello world!", Translation.getStrippedString(false));
        assertEquals("Hello <a>world</a>!", Translation.toString());
    }

    @Test
    public void testTranslationWithDiscordantTagSpacing_FalseTrue() {
        Translation Translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<a>", true, false, 1),
                Tag.fromText("</a>", false, true, 2),
        });

        assertEquals("Hello world!", Translation.getStrippedString(false));
        assertEquals("Hello <a>world</a>!", Translation.toString());
    }

    @Test
    public void testTranslationWithDiscordantTagSpacing_TrueFalse() {
        Translation Translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", true),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<a>", true, false, 1),
                Tag.fromText("</a>", false, false, 2),
        });

        assertEquals("Hello world !", Translation.getStrippedString(false));
        assertEquals("Hello <a>world</a> !", Translation.toString());
    }

    @Test
    public void testTranslationWithSpacedTagList() {
        Translation Translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<a>", true, true, 1),
                Tag.fromText("<b>", true, true, 1),
                Tag.fromText("</a>", true, true, 1),
                Tag.fromText("</b>", true, true, 1),
        });

        assertEquals("Hello world!", Translation.getStrippedString(false));
        assertEquals("Hello <a><b></a></b>world!", Translation.toString());
    }

    @Test
    public void testTranslationWithTagListSpaceInMiddle() {
        Translation Translation = translation(new Token[]{
                new Token("Hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("</a>", true, true, 1),
                Tag.fromText("<b>", true, true, 1),
                Tag.fromText("</b>", true, true, 1),
        });

        assertEquals("Hello world!", Translation.getStrippedString(false));
        assertEquals("Hello</a> <b></b>world!", Translation.toString());
    }

    @Test
    public void testTranslationWithTagInUnbreakableTokenList() {
        Translation Translation = translation(new Token[]{
                new Token("That", false),
                new Token("'s", true),
                new Token("it", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<b>", true, true, 1),
                Tag.fromText("</b>", true, true, 2),
        });

        assertEquals("That's it!", Translation.getStrippedString(false));
        assertEquals("That<b>'s</b> it!", Translation.toString());
    }

    @Test
    public void testTranslationWithSpacedCommentTag() {
        Translation Translation = translation(new Token[]{
                new Token("This", true),
                new Token("is", true),
                new Token("XML", true),
                new Token("comment", false),
        }, new Tag[]{
                Tag.fromText("<!--", true, true, 2),
                Tag.fromText("-->", true, false, 4),
        });

        assertEquals("This is XML comment", Translation.getStrippedString(false));
        assertEquals("This is <!-- XML comment -->", Translation.toString());
    }

    @Test
    public void testTranslationWithSpacedCommentTag_NoLeadingSpace() {
        Translation Translation = translation(new Token[]{
                new Token("This", true),
                new Token("is", true),
                new Token("XML", true),
                new Token("comment", false),
        }, new Tag[]{
                Tag.fromText("<!--", false, true, 2),
                Tag.fromText("-->", true, false, 4),
        });

        assertEquals("This is XML comment", Translation.getStrippedString(false));
        assertEquals("This is<!-- XML comment -->", Translation.toString());
    }

    @Test
    public void testTranslationWithSpacedCommentTag_TrailingSpace() {
        Translation Translation = translation(new Token[]{
                new Token("This", true),
                new Token("is", true),
                new Token("XML", true),
                new Token("comment", false),
        }, new Tag[]{
                Tag.fromText("<!--", false, true, 2),
                Tag.fromText("-->", true, true, 4),
        });

        assertEquals("This is XML comment", Translation.getStrippedString(false));
        assertEquals("This is<!-- XML comment -->", Translation.toString());
    }

}
