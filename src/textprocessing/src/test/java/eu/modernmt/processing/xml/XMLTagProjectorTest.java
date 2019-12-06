package eu.modernmt.processing.xml;

import eu.modernmt.model.*;
import eu.modernmt.processing.tags.projection.TagProjector;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class XMLTagProjectorTest {

    @Test
    public void testOpeningNotEmptyMonotone() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", null," "),
                new Word("world", " ",null, true, true),
                new Word("!", null,null, true, false),
        }, new Tag[]{
                XMLTag.fromText("<b>", " ", null, 1),
                XMLTag.fromText("</b>", null, null, 2),
        });

        Translation translation = new Translation(new Word[]{
                new Word("ciao", null, " "),
                new Word("mondo", " ", null),
                new Word("!", null, null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("ciao <b>mondo</b>!", translation.toString());
        assertEquals("ciao mondo!", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<b>", " ", null, 1),
                XMLTag.fromText("</b>", null, null, 2),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNotEmptyNonMonotone() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", null," "),
                new Word("world", null,null),
                new Word("!", null,null),
        }, new Tag[]{
                XMLTag.fromText("<b>", " ", null, 1),
                XMLTag.fromText("</b>", null, null, 2),
        });

        Translation translation = new Translation(new Word[]{
                new Word("mondo", null," "),
                new Word("ciao", " ",null),
                new Word("!", null, null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 1},
                {1, 0},
                {2, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("<b>mondo</b> ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<b>", null, null, 0),
                XMLTag.fromText("</b>", null, " ", 1),
        }, translation.getTags());
    }

    @Test
    @Ignore
    public void testEmptyTag() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null," "),
                new Word("with", " ", " "),
                new Word("an", " ", " "),
                new Word("empty", " ", null),
                new Word("tag", " ", " "),
        }, new Tag[]{
                XMLTag.fromText("<empty/>", " ", null, 3),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", " ", " "),
                new Word("con", " ", " "),
                new Word("un", " ", " "),
                new Word("tag", " ", " "),
                new Word("empty", " ", " "),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 1},
                {3, 4},
                {4, 3},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio con un tag <empty/>empty", translation.toString());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<empty/>", " ", null, 4),
        }, translation.getTags());
        assertEquals("Esempio con un tag empty", translation.toString(false, false));
    }

    @Test
    public void testOpeningEmptyMonotone() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", null, " "),
                new Word("world",  null, null),
                new Word("!", null, null),
        }, new Tag[]{
                XMLTag.fromText("<g>", " ", null, 1),
                XMLTag.fromText("</g>", null, null, 1),
        });

        Translation translation = new Translation(new Word[]{
                new Word("ciao", null," "),
                new Word("mondo", " ", null),
                new Word("!", null,null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("ciao <g></g> mondo!", translation.toString());
        assertEquals("ciao mondo!", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<g>", " ", null, 1),
                XMLTag.fromText("</g>", null, " ", 1),
        }, translation.getTags());
    }

    @Test
    @Ignore
    public void testOpeningEmptyNonMonotone() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", null," "),
                new Word("world", " ",null),
                new Word("!", null,null),
        }, new Tag[]{
                XMLTag.fromText("<g>", " ", null, 1),
                XMLTag.fromText("</g>", null, null, 1),
        });

        Translation translation = new Translation(new Word[]{
                new Word("mondo", null, " "),
                new Word("ciao", " ", null),
                new Word("!", null, null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 1},
                {1, 0},
                {2, 2},
        }));

        new TagProjector().project(translation);
        //System.out.println(translation.getSource().toString());
        assertEquals("<g></g>mondo ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<g>", null, null, 0),
                XMLTag.fromText("</g>", null, null, 0),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNonClosing() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " "),
                new Word("with", " ", " "),
                new Word("a", null, " "),
                new Word("malformed", " ", " "),
                new Word("tag", " ",null),
        }, new Tag[]{
                XMLTag.fromText("<open>", " ", null, 2),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " "),
                new Word("con", " ", " "),
                new Word("un", " ", " "),
                new Word("tag", " ", " "),
                new Word("malformato", " ",null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 4},
                {4, 3},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio con <open>un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<open>", " ", null, 2),
        }, translation.getTags());
    }

    @Test
    public void testClosingNonOpening() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " "),
                new Word("with", " ", null),
                new Word("a", " ", " "),
                new Word("malformed", " ", " "),
                new Word("tag", " ",null),
        }, new Tag[]{
                XMLTag.fromText("</close>", null, " ", 2),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " "),
                new Word("con", " ", " "),
                new Word("un", " ", " "),
                new Word("tag", " ", " "),
                new Word("malformato", " ", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 4},
                {4, 3},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio con</close> un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("</close>", null, " ", 2),
        }, translation.getTags());
    }

    @Test
    public void testEmbeddedTags() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " "),
                new Word("with", null, " "),
                new Word("nested", " ", null),
                new Word("tag", null, null),
        }, new Tag[]{
                XMLTag.fromText("<a>", " ", null, 1),
                XMLTag.fromText("<b>", " ", null, 3),
                XMLTag.fromText("</b>", null, null, 4),
                XMLTag.fromText("</a>", null, null, 4),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", " ", " "),
                new Word("con", " ", " "),
                new Word("tag", " ", " "),
                new Word("innestati", " ", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio <a>con <b>tag</b> innestati</a>", translation.toString());
        assertEquals("Esempio con tag innestati", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", " ", null, 1),
                XMLTag.fromText("<b>", " ", null, 2),
                XMLTag.fromText("</b>", null, " ", 3),
                XMLTag.fromText("</a>", null, null, 4),
        }, translation.getTags());
    }

    @Test
    public void testSpacedXMLCommentTags() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " "),
                new Word("with", " "," "),
                new Word("XML", " "," "),
                new Word("comment", " ",null),
        }, new Tag[]{
                XMLTag.fromText("<!--", " ", " ", 2),
                XMLTag.fromText("-->", " ", null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " "),
                new Word("con", " ", " "),
                new Word("commenti", " ", " "),
                new Word("XML", " ", " "),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio con <!-- commenti XML -->", translation.toString());
        assertEquals("Esempio con commenti XML", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<!--", " ", " ", 2),
                XMLTag.fromText("-->", " ", null, 4),
        }, translation.getTags());
    }

    @Test
    public void testNotSpacedXMLCommentTags() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " "),
                new Word("with", " ", " "),
                new Word("XML", null, " "),
                new Word("comment", " ", null),
        }, new Tag[]{
                XMLTag.fromText("<!--", " ", null, 2),
                XMLTag.fromText("-->", null, null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " "),
                new Word("con", " ", " "),
                new Word("commenti", " ", " "),
                new Word("XML", " ", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        new TagProjector().project(translation);

        assertEquals("Esempio con <!--commenti XML-->", translation.toString());
        assertEquals("Esempio con commenti XML", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<!--", " ", null, 2),
                XMLTag.fromText("-->", null, null, 4),
        }, translation.getTags());
    }

    @Test
    public void testSingleXMLComment() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("This", null," "),
                new Word("is", " ", " "),
                new Word("a", " ", " "),
                new Word("test", " ",null),
        }, new Tag[]{
                XMLTag.fromText("<!--", null, null, 0),
                XMLTag.fromText("-->", null, null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Questo", null, " "),
                new Word("è", " ", " "),
                new Word("un", " ", " "),
                new Word("esempio", " ", null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 3},
        }));

        new TagProjector().project(translation);

        assertEquals("<!--Questo è un esempio-->", translation.toString());
        assertEquals("Questo è un esempio", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<!--", null, null, 0),
                XMLTag.fromText("-->", null, null, 4),
        }, translation.getTags());
    }

    @Test
    public void testDTDTags() throws Throwable {
        Sentence source = new Sentence(new Word[]{
                new Word("Test", " ",null),
        }, new Tag[]{
                XMLTag.fromText("<!ENTITY key=\"value\">", null, " ", 0),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Prova", null, null),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0}
        }));

        new TagProjector().project(translation);

        assertEquals("<!ENTITY key=\"value\"> Prova", translation.toString());
        assertEquals("Prova", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<!ENTITY key=\"value\">", null, " ", 0),
        }, translation.getTags());
    }

    @Test
    public void testOnlyTags() throws Throwable {
        Sentence source = new Sentence(null, new Tag[]{
                XMLTag.fromText("<a>", null, null, 0),
                XMLTag.fromText("</a>", null, null, 0),
        });

        Translation translation = new Translation(null, source, null);

        new TagProjector().project(translation);

        assertEquals("<a></a>", translation.toString());
        assertTrue(translation.toString(false, false).isEmpty());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", null, null, 0),
                XMLTag.fromText("</a>", null, null, 0),
        }, translation.getTags());
    }

}
