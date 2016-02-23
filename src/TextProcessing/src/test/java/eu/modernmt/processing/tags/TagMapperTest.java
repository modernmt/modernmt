package eu.modernmt.processing.tags;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by davide on 22/02/16.
 */
public class TagMapperTest {


    @Test
    public void testOpeningNotEmptyMonotone() {
        Sentence source = new Sentence(new Token[]{
                new Token("hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                new Tag("b", "<b>", true, false, 1, Tag.Type.OPENING_TAG),
                new Tag("b", "</b>", false, false, 2, Tag.Type.CLOSING_TAG),
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

        TagMapper.remap(source, translation);

        assertEquals("ciao <b>mondo</b>!", translation.toString());
        assertEquals("ciao mondo!", translation.getStrippedString());
        assertArrayEquals(new Tag[]{
                new Tag("b", "<b>", true, false, 1, Tag.Type.OPENING_TAG),
                new Tag("b", "</b>", false, false, 2, Tag.Type.CLOSING_TAG),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNotEmptyNonMonotone() {
        Sentence source = new Sentence(new Token[]{
                new Token("hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                new Tag("b", "<b>", true, false, 1, Tag.Type.OPENING_TAG),
                new Tag("b", "</b>", false, false, 2, Tag.Type.CLOSING_TAG),
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

        TagMapper.remap(source, translation);

        assertEquals("<b>mondo</b> ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.getStrippedString());
        assertArrayEquals(new Tag[]{
                new Tag("b", "<b>", true, false, 0, Tag.Type.OPENING_TAG),
                new Tag("b", "</b>", false, false, 1, Tag.Type.CLOSING_TAG),
        }, translation.getTags());
    }

    @Test
    public void testEmptyTag() {
        Sentence source = new Sentence(new Token[]{
                new Token("Example", true),
                new Token("with", true),
                new Token("an", true),
                new Token("empty", true),
                new Token("tag", true),
        }, new Tag[]{
                new Tag("empty", "<empty />", true, false, 3, Tag.Type.EMPTY_TAG),
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

        TagMapper.remap(source, translation);

        assertEquals("Esempio con un tag <empty />empty", translation.toString());
        assertArrayEquals(new Tag[]{
                new Tag("empty", "<empty />", true, false, 4, Tag.Type.EMPTY_TAG),
        }, translation.getTags());
        assertEquals("Esempio con un tag empty", translation.getStrippedString());
    }

    @Test
    public void testOpeningEmptyMonotone() {
        Sentence source = new Sentence(new Token[]{
                new Token("hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                new Tag("g", "<g>", true, false, 1, Tag.Type.OPENING_TAG),
                new Tag("g", "</g>", false, false, 1, Tag.Type.CLOSING_TAG),
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

        TagMapper.remap(source, translation);

        assertEquals("ciao <g></g>mondo!", translation.toString());
        assertEquals("ciao mondo!", translation.getStrippedString());
        assertArrayEquals(new Tag[]{
                new Tag("g", "<g>", true, false, 1, Tag.Type.OPENING_TAG),
                new Tag("g", "</g>", false, false, 1, Tag.Type.CLOSING_TAG),
        }, translation.getTags());
    }

    @Test
    public void testOpeningEmptyNonMonotone() {
        Sentence source = new Sentence(new Token[]{
                new Token("hello", true),
                new Token("world", false),
                new Token("!", false),
        }, new Tag[]{
                new Tag("g", "<g>", true, false, 1, Tag.Type.OPENING_TAG),
                new Tag("g", "</g>", false, false, 1, Tag.Type.CLOSING_TAG),
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

        TagMapper.remap(source, translation);

        assertEquals("<g></g>mondo ciao!", translation.toString());
        assertEquals("mondo ciao!", translation.getStrippedString());
        assertArrayEquals(new Tag[]{
                new Tag("g", "<g>", true, false, 0, Tag.Type.OPENING_TAG),
                new Tag("g", "</g>", false, false, 0, Tag.Type.CLOSING_TAG),
        }, translation.getTags());
    }

    @Test
    public void testOpeningNonClosing() {
        Sentence source = new Sentence(new Token[]{
                new Token("Example", true),
                new Token("with", true),
                new Token("a", true),
                new Token("malformed", true),
                new Token("tag", true),
        }, new Tag[]{
                new Tag("open", "<open>", true, false, 2, Tag.Type.OPENING_TAG),
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

        TagMapper.remap(source, translation);

        assertEquals("Esempio con <open>un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.getStrippedString());
        assertArrayEquals(new Tag[]{
                new Tag("open", "<open>", true, false, 2, Tag.Type.OPENING_TAG),
        }, translation.getTags());
    }

    @Test
    public void testClosingNonOpening() {
        Sentence source = new Sentence(new Token[]{
                new Token("Example", true),
                new Token("with", true),
                new Token("a", true),
                new Token("malformed", true),
                new Token("tag", true),
        }, new Tag[]{
                new Tag("close", "</close>", false, true, 2, Tag.Type.CLOSING_TAG),
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

        TagMapper.remap(source, translation);

        assertEquals("Esempio con</close> un tag malformato", translation.toString());
        assertEquals("Esempio con un tag malformato", translation.getStrippedString());
        assertArrayEquals(new Tag[]{
                new Tag("close", "</close>", false, true, 2, Tag.Type.CLOSING_TAG),
        }, translation.getTags());
    }


    @Test
    public void testEmbeddedTags() {
        Sentence source = new Sentence(new Token[]{
                new Token("Example", true),
                new Token("with", true),
                new Token("nested", true),
                new Token("tag", false),
        }, new Tag[]{
                new Tag("a", "<a>", true, false, 1, Tag.Type.OPENING_TAG),
                new Tag("b", "<b>", true, false, 3, Tag.Type.OPENING_TAG),
                new Tag("b", "</b>", false, true, 4, Tag.Type.CLOSING_TAG),
                new Tag("a", "</a>", false, false, 4, Tag.Type.CLOSING_TAG),
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

        TagMapper.remap(source, translation);

        assertEquals("Esempio <a>con <b>tag</b> innestati</a>", translation.toString());
        assertEquals("Esempio con tag innestati", translation.getStrippedString());
        assertArrayEquals(new Tag[]{
                new Tag("a", "<a>", true, false, 1, Tag.Type.OPENING_TAG),
                new Tag("b", "<b>", true, false, 2, Tag.Type.OPENING_TAG),
                new Tag("b", "</b>", false, true, 3, Tag.Type.CLOSING_TAG),
                new Tag("a", "</a>", false, false, 4, Tag.Type.CLOSING_TAG),
        }, translation.getTags());
    }

}
