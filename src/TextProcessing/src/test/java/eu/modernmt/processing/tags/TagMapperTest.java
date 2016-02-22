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
    public void testSimple() {
        Sentence source = new Sentence(new Token[]{
                new Token("hello", true),
                new Token("world", true),
        }, new Tag[]{
                new Tag("e", "</e>", false, true, 0, Tag.Type.CLOSING_TAG),
                new Tag("b", "<b>", true, false, 2, Tag.Type.OPENING_TAG),
        });

        Translation translation = new Translation(new Token[]{
                new Token("ciao", true),
                new Token("mondo", true),
        }, source, new int[][]{
                {0, 0},
                {1, 1},
        });

        TagMapper.remap(source, translation);

        assertEquals("</e> ciao mondo <b>", translation.toString());
        assertEquals("ciao mondo", translation.getStrippedString());
    }

    @Test
    public void testEmptyTag() {
        Sentence source = new Sentence(new Token[]{
                new Token("hello", true),
                new Token("world", true),
        }, new Tag[]{
                new Tag("world", "<world />", true, false, 1, Tag.Type.EMPTY_TAG),
        });
        Translation translation = new Translation(new Token[]{
                new Token("mondo", true),
                new Token("ciao", true),
        }, source, new int[][]{
                {0, 1},
                {1, 0},
        });


        TagMapper.remap(source, translation);

        assertEquals("<world />mondo ciao", translation.toString());
        assertArrayEquals(new Tag[] {
                new Tag("world", "<world />", true, false, 0, Tag.Type.EMPTY_TAG),
        }, translation.getTags());
    }

}
