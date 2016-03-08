package eu.modernmt.processing.tags;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.xml.TagMapper;
import org.junit.Test;

import static org.junit.Assert.*;

public class TagMapperTest {

    @Test
    public void testOpeningNotEmptyMonotone() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<b>", true, false, 1),
                Tag.fromText("</b>", false, false, 2),
        });

        Translation translation = new Translation(new Token[]{
                new Token("ciao", true),
                new Token("mondo", false),
                new Token("!", false),
        }, source, new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
        });

        new TagMapper().call(translation);

        assertEquals("ciao <b>mondo</b>!", translation.toString());
        assertEquals("ciao mondo!", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<b>", true, false, 1),
                Tag.fromText("</b>", false, false, 2),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNotEmptyNonMonotone() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<b>", true, false, 1),
                Tag.fromText("</b>", false, false, 2),
        });

        Translation translation = new Translation(new Token[]{
                new Token("mondo", true),
                new Token("ciao", false),
                new Token("!", false),
        }, source, new int[][]{
                {0, 1},
                {1, 0},
                {2, 2},
        });

        new TagMapper().call(translation);

        assertEquals("<b>mondo</b> ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<b>", true, false, 0),
                Tag.fromText("</b>", false, false, 1),
        }, translation.getTags());
    }

    @Test
    public void testEmptyTag() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("Example", true),
                new Token("with", true),
                new Token("an", true),
                new Token("empty", true),
                new Token("tag", true),
        }, new Tag[]{
                Tag.fromText("<empty/>", true, false, 3),
        });
        Translation translation = new Translation(new Token[]{
                new Token("Esempio", true),
                new Token("con", true),
                new Token("un", true),
                new Token("tag", true),
                new Token("empty", true),
        }, source, new int[][]{
                {0, 0},
                {1, 1},
                {2, 1},
                {3, 4},
                {4, 3},
        });

        new TagMapper().call(translation);

        assertEquals("Esempio con un tag <empty/>empty", translation.toString());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<empty/>", true, false, 4),
        }, translation.getTags());
        assertEquals("Esempio con un tag empty", translation.getStrippedString(false));
    }

    @Test
    public void testOpeningEmptyMonotone() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<g>", true, false, 1),
                Tag.fromText("</g>", false, false, 1),
        });

        Translation translation = new Translation(new Token[]{
                new Token("ciao", true),
                new Token("mondo", false),
                new Token("!", false),
        }, source, new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
        });

        new TagMapper().call(translation);

        assertEquals("ciao <g></g>mondo!", translation.toString());
        assertEquals("ciao mondo!", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<g>", true, false, 1),
                Tag.fromText("</g>", false, false, 1),
        }, translation.getTags());
    }

    @Test
    public void testOpeningEmptyNonMonotone() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                Tag.fromText("<g>", true, false, 1),
                Tag.fromText("</g>", false, false, 1),
        });

        Translation translation = new Translation(new Token[]{
                new Token("mondo", true),
                new Token("ciao", false),
                new Token("!", false),
        }, source, new int[][]{
                {0, 1},
                {1, 0},
                {2, 2},
        });

        new TagMapper().call(translation);

        assertEquals("<g></g>mondo ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<g>", true, false, 0),
                Tag.fromText("</g>", false, false, 0),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNonClosing() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("Example", true),
                new Token("with", true),
                new Token("a", true),
                new Token("malformed", true),
                new Token("tag", true),
        }, new Tag[]{
                Tag.fromText("<open>", true, false, 2),
        });
        Translation translation = new Translation(new Token[]{
                new Token("Esempio", true),
                new Token("con", true),
                new Token("un", true),
                new Token("tag", true),
                new Token("malformato", true),
        }, source, new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 4},
                {4, 3},
        });

        new TagMapper().call(translation);

        assertEquals("Esempio con <open>un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<open>", true, false, 2),
        }, translation.getTags());
    }

    @Test
    public void testClosingNonOpening() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("Example", true),
                new Token("with", true),
                new Token("a", true),
                new Token("malformed", true),
                new Token("tag", true),
        }, new Tag[]{
                Tag.fromText("</close>", false, true, 2),
        });
        Translation translation = new Translation(new Token[]{
                new Token("Esempio", true),
                new Token("con", true),
                new Token("un", true),
                new Token("tag", true),
                new Token("malformato", true),
        }, source, new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 4},
                {4, 3},
        });

        new TagMapper().call(translation);

        assertEquals("Esempio con</close> un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("</close>", false, true, 2),
        }, translation.getTags());
    }

    @Test
    public void testEmbeddedTags() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("Example", true),
                new Token("with", true),
                new Token("nested", true),
                new Token("tag", false),
        }, new Tag[]{
                Tag.fromText("<a>", true, false, 1),
                Tag.fromText("<b>", true, false, 3),
                Tag.fromText("</b>", false, true, 4),
                Tag.fromText("</a>", false, true, 4),
        });
        Translation translation = new Translation(new Token[]{
                new Token("Esempio", true),
                new Token("con", true),
                new Token("tag", true),
                new Token("innestati", false),
        }, source, new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        });

        new TagMapper().call(translation);

        assertEquals("Esempio <a>con <b>tag</b> innestati</a>", translation.toString());
        assertEquals("Esempio con tag innestati", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", true, false, 1),
                Tag.fromText("<b>", true, false, 2),
                Tag.fromText("</b>", false, true, 3),
                Tag.fromText("</a>", false, true, 4),
        }, translation.getTags());
    }

    @Test
    public void testSpacedXMLCommentTags() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("Example", true),
                new Token("with", true),
                new Token("XML", true),
                new Token("comment", false),
        }, new Tag[]{
                Tag.fromText("<!--", true, true, 2),
                Tag.fromText("-->", true, false, 4),
        });

        Translation translation = new Translation(new Token[]{
                new Token("Esempio", true),
                new Token("con", true),
                new Token("commenti", true),
                new Token("XML", true),
        }, source, new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        });

        new TagMapper().call(translation);

        assertEquals("Esempio con <!-- commenti XML -->", translation.toString());
        assertEquals("Esempio con commenti XML", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!--", true, true, 2),
                Tag.fromText("-->", true, false, 4),
        }, translation.getTags());
    }

    @Test
    public void testNotSpacedXMLCommentTags() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("Example", true),
                new Token("with", true),
                new Token("XML", true),
                new Token("comment", false),
        }, new Tag[]{
                Tag.fromText("<!--", true, false, 2),
                Tag.fromText("-->", false, false, 4),
        });

        Translation translation = new Translation(new Token[]{
                new Token("Esempio", true),
                new Token("con", true),
                new Token("commenti", true),
                new Token("XML", true),
        }, source, new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        });

        new TagMapper().call(translation);

        assertEquals("Esempio con <!--commenti XML-->", translation.toString());
        assertEquals("Esempio con commenti XML", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!--", true, false, 2),
                Tag.fromText("-->", false, false, 4),
        }, translation.getTags());
    }

    @Test
    public void testSingleXMLComment() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("This", true),
                new Token("is", true),
                new Token("a", true),
                new Token("test", false),
        }, new Tag[]{
                Tag.fromText("<!--", false, false, 0),
                Tag.fromText("-->", false, false, 4),
        });

        Translation translation = new Translation(new Token[]{
                new Token("Questo", true),
                new Token("è", true),
                new Token("un", true),
                new Token("esempio", false),
        }, source, new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 3},
        });

        new TagMapper().call(translation);

        assertEquals("<!--Questo è un esempio-->", translation.toString());
        assertEquals("Questo è un esempio", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!--", false, false, 0),
                Tag.fromText("-->", false, false, 4),
        }, translation.getTags());
    }

    @Test
    public void testDTDTags() throws ProcessingException {
        Sentence source = new Sentence(new Token[]{
                new Token("Test", false),
        }, new Tag[]{
                Tag.fromText("<!ENTITY key=\"value\">", false, true, 0),
        });

        Translation translation = new Translation(new Token[]{
                new Token("Prova", false),
        }, source, new int[][] {
                {0, 0}
        });

        new TagMapper().call(translation);

        assertEquals("<!ENTITY key=\"value\"> Prova", translation.toString());
        assertEquals("Prova", translation.getStrippedString(false));
        assertArrayEquals(new Tag[]{
                Tag.fromText("<!ENTITY key=\"value\">", false, true, 0),
        }, translation.getTags());
    }

    @Test
    public void testOnlyTags() throws ProcessingException {
        Sentence source = new Sentence(null, new Tag[]{
                Tag.fromText("<a>", false, false, 0),
                Tag.fromText("</a>", false, false, 0),
        });

        Translation translation = new Translation(null, source, null);

        new TagMapper().call(translation);

        assertEquals("<a></a>", translation.toString());
        assertTrue(translation.getStrippedString(false).isEmpty());
        assertArrayEquals(new Tag[]{
                Tag.fromText("<a>", false, false, 0),
                Tag.fromText("</a>", false, false, 0),
        }, translation.getTags());
    }

}
