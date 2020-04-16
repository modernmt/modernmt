package eu.modernmt.processing.xml;

import eu.modernmt.model.*;
import eu.modernmt.processing.tags.projection.TagProjector;
import org.junit.Test;

import static org.junit.Assert.*;

public class XMLTagProjectorTest {

    @Test
    public void testOpeningNotEmptyMonotone() {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", null," ", false, true),
                new Word("world", " ",null, true, true),
                new Word("!", null,null, true, false),
        }, new Tag[]{
                XMLTag.fromText("<b>", " ", null, 1),
                XMLTag.fromText("</b>", null, null, 2),
        });

        Translation translation = new Translation(new Word[]{
                new Word("ciao", null, " ", false, true),
                new Word("mondo", " ", " ", true, false),
                new Word("!", " ", null, false,false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
        }));

        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("ciao <b>mondo</b>!", translation.toString());
        assertEquals("ciao mondo!", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<b>", " ", null, 1),
                XMLTag.fromText("</b>", null, null, 2),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNotEmptyNonMonotone() {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", null," ", false, true),
                new Word("world", null,null, true, false),
                new Word("!", null,null, false, false),
        }, new Tag[]{
                XMLTag.fromText("<b>", " ", null, 1),
                XMLTag.fromText("</b>", null, null, 2),
        });

        Translation translation = new Translation(new Word[]{
                new Word("mondo", null," ", false, true),
                new Word("ciao", " "," ", true,false),
                new Word("!", " ", null,false,false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 1},
                {1, 0},
                {2, 2},
        }));

        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("<b>mondo</b> ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<b>", null, null, 0),
                XMLTag.fromText("</b>", null, " ", 1),
        }, translation.getTags());
    }

    @Test
    public void testEmptyTag() {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null," ",false,true),
                new Word("with", " ", " ", true, true),
                new Word("an", " ", " ", true, true),
                new Word("empty", " ", null, true, true),
                new Word("tag", " ", " ", true, false),
        }, new Tag[]{
                XMLTag.fromText("<empty/>", " ", null, 3),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " ",false, true),
                new Word("con", " ", " ", true, true),
                new Word("un", " ", " ", true, true),
                new Word("tag", " ", " ", true, true),
                new Word("empty", " ", null, true, false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 4},
                {4, 3},
        }));

        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("Esempio con un tag <empty/>empty", translation.toString());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<empty/>", " ", null, 4),
        }, translation.getTags());
        assertEquals("Esempio con un tag empty", translation.toString(false, false));
    }

    @Test
    public void testOpeningEmptyMonotone() {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", null, " ", true, true),
                new Word("world",  null, null, true, false),
                new Word("!", null, null,false,false),
        }, new Tag[]{
                XMLTag.fromText("<g>", " ", null, 1),
                XMLTag.fromText("</g>", null, null, 1),
        });

        Translation translation = new Translation(new Word[]{
                new Word("ciao", null," ",false,true),
                new Word("mondo", " ", " ",true,false),
                new Word("!", " ",null,false,false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
        }));

        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("ciao <g></g>mondo!", translation.toString());
        assertEquals("ciao mondo!", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<g>", " ", null, 1),
                XMLTag.fromText("</g>", null, null, 1),
        }, translation.getTags());
    }

    @Test
    public void testOpeningEmptyNonMonotone() {
        Sentence source = new Sentence(new Word[]{
                new Word("hello", null," ",false,true),
                new Word("world", " ",null,true,false),
                new Word("!", null,null,false,false),
        }, new Tag[]{
                XMLTag.fromText("<g>", " ", null, 1),
                XMLTag.fromText("</g>", null, null, 1),
        });

        Translation translation = new Translation(new Word[]{
                new Word("mondo", null, " ",false,true),
                new Word("ciao", " ", " ",true,false),
                new Word("!", " ", null,false,false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 1},
                {1, 0},
                {2, 2},
        }));


        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("<g></g>mondo ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<g>", null, null, 0),
                XMLTag.fromText("</g>", null, null, 0),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNonClosing() {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " ",false,true),
                new Word("with", " ", " ",true,true),
                new Word("a", null, " ",true,true),
                new Word("malformed", " ", " ",true,true),
                new Word("tag", " ",null,true,false),
        }, new Tag[]{
                XMLTag.fromText("<open>", " ", null, 2),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " ",false,true),
                new Word("con", " ", " ",true,true),
                new Word("un", " ", " ",true,true),
                new Word("tag", " ", " ",true,true),
                new Word("malformato", " ",null,true,false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 4},
                {4, 3},
        }));

        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("Esempio con <open>un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<open>", " ", null, 2),
        }, translation.getTags());
    }

    @Test
    public void testClosingNonOpening() {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " ",false,true),
                new Word("with", " ", null,true,true),
                new Word("a", " ", " ",true,true),
                new Word("malformed", " ", " ",true,true),
                new Word("tag", " ",null,true,false),
        }, new Tag[]{
                XMLTag.fromText("</close>", null, " ", 2),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " ",false,true),
                new Word("con", " ", " ",true,true),
                new Word("un", " ", " ",true,true),
                new Word("tag", " ", " ",true,true),
                new Word("malformato", " ", null,true,false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 4},
                {4, 3},
        }));

        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("Esempio con</close> un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("</close>", null, " ", 2),
        }, translation.getTags());
    }

    @Test
    public void testEmbeddedTags() {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " ",false,true),
                new Word("with", " ", " ",true,true),
                new Word("nested", null, " ",true,true),
                new Word("tag", null, null,true,false),
        }, new Tag[]{
                XMLTag.fromText("<a>", " ", null, 1),
                XMLTag.fromText("<b>", " ", null, 3),
                XMLTag.fromText("</b>", null, null, 4),
                XMLTag.fromText("</a>", null, null, 4),
        });
        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " ",false,true),
                new Word("con", " ", " ",true,true),
                new Word("tag", " ", " ",true,true),
                new Word("innestati", " ", null,true,false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        translation.fixWordSpacing();
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
    public void testSpacedXMLCommentTags() {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " ",false,true),
                new Word("with", " "," ",true,true),
                new Word("XML", " "," ",true,true),
                new Word("comment", " ",null,true,false),
        }, new Tag[]{
                XMLTag.fromText("<!--", " ", " ", 2),
                XMLTag.fromText("-->", " ", null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " ",false,true),
                new Word("con", " ", " ",true,true),
                new Word("commenti", " ", " ",true,true),
                new Word("XML", " ", null,true,false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("Esempio con <!-- commenti XML -->", translation.toString());
        assertEquals("Esempio con commenti XML", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<!--", " ", " ", 2),
                XMLTag.fromText("-->", " ", null, 4),
        }, translation.getTags());
    }

    @Test
    public void testNotSpacedXMLCommentTags() {
        Sentence source = new Sentence(new Word[]{
                new Word("Example", null, " ",false,true),
                new Word("with", " ", " ",true,true),
                new Word("XML", null, " ",true,true),
                new Word("comment", " ", null,true,true),
        }, new Tag[]{
                XMLTag.fromText("<!--", " ", null, 2),
                XMLTag.fromText("-->", null, null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Esempio", null, " ",false,true),
                new Word("con", " ", " ",true,true),
                new Word("commenti", " ", " ",true,true),
                new Word("XML", " ", null,true,false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 3},
                {3, 2},
        }));

        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("Esempio con <!--commenti XML-->", translation.toString());
        assertEquals("Esempio con commenti XML", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<!--", " ", null, 2),
                XMLTag.fromText("-->", null, null, 4),
        }, translation.getTags());
    }

    @Test
    public void testSingleXMLComment() {
        Sentence source = new Sentence(new Word[]{
                new Word("This", null," ",false,true),
                new Word("is", " ", " ",true,true),
                new Word("a", " ", " ",true,true),
                new Word("test", " ",null,true,false),
        }, new Tag[]{
                XMLTag.fromText("<!--", null, null, 0),
                XMLTag.fromText("-->", null, null, 4),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Questo", null, " ",false,true),
                new Word("è", " ", " ",true,true),
                new Word("un", " ", " ",true,true),
                new Word("esempio", " ", null,true,false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0},
                {1, 1},
                {2, 2},
                {3, 3},
        }));

        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("<!--Questo è un esempio-->", translation.toString());
        assertEquals("Questo è un esempio", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<!--", null, null, 0),
                XMLTag.fromText("-->", null, null, 4),
        }, translation.getTags());
    }

    @Test
    public void testDTDTags() {
        Sentence source = new Sentence(new Word[]{
                new Word("Test", " ",null,false,false),
        }, new Tag[]{
                XMLTag.fromText("<!ENTITY key=\"value\">", null, " ", 0),
        });

        Translation translation = new Translation(new Word[]{
                new Word("Prova", null, null,false,false),
        }, source, Alignment.fromAlignmentPairs(new int[][]{
                {0, 0}
        }));

        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("<!ENTITY key=\"value\"> Prova", translation.toString());
        assertEquals("Prova", translation.toString(false, false));
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<!ENTITY key=\"value\">", null, " ", 0),
        }, translation.getTags());
    }

    @Test
    public void testOnlyTags() {
        Sentence source = new Sentence(null, new Tag[]{
                XMLTag.fromText("<a>", null, null, 0),
                XMLTag.fromText("</a>", null, null, 0),
        });

        Translation translation = new Translation(null, source, null);

        translation.fixWordSpacing();
        new TagProjector().project(translation);

        assertEquals("<a></a>", translation.toString());
        assertTrue(translation.toString(false, false).isEmpty());
        assertArrayEquals(new Tag[]{
                XMLTag.fromText("<a>", null, null, 0),
                XMLTag.fromText("</a>", null, null, 0),
        }, translation.getTags());
    }

}
