package eu.modernmt.decoder.neural.memory;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static eu.modernmt.decoder.neural.memory.TestData.EN__IT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 03/08/17.
 */
public class LuceneTranslationMemoryTest_searchWithOwners {

    private static UUID owner1 = new UUID(0, 1);
    private static UUID owner2 = new UUID(0, 2);

    private TLuceneTranslationMemory memory;

    @Before
    public void setup() throws Throwable {
        this.memory = new TLuceneTranslationMemory();

        memory.onDataReceived(Collections.singleton(TestData.tu(0, 0L, null, 1L, EN__IT, "Hello world 1", "Ciao mondo 1", null)));
        memory.onDataReceived(Collections.singleton(TestData.tu(0, 1L, null, 2L, EN__IT, "Hello world 2", "Ciao mondo 2", null)));
        memory.onDataReceived(Collections.singleton(TestData.tu(0, 2L, owner1, 11L, EN__IT, "Hello world 1", "Ciao mondo 1", null)));
        memory.onDataReceived(Collections.singleton(TestData.tu(0, 3L, owner1, 12L, EN__IT, "Hello world 2", "Ciao mondo 2", null)));
        memory.onDataReceived(Collections.singleton(TestData.tu(0, 4L, owner2, 21L, EN__IT, "Hello world 1", "Ciao mondo 1", null)));
        memory.onDataReceived(Collections.singleton(TestData.tu(0, 5L, owner2, 22L, EN__IT, "Hello world 2", "Ciao mondo 2", null)));
    }

    @After
    public void teardown() {
        this.memory.close();
        this.memory = null;
    }

    private static boolean contains(ScoreEntry[] entries, long memory, LanguagePair direction, String sentence, String translation) {
        ScoreEntry target = new ScoreEntry(memory, direction, sentence.split(" "), translation.split(" "));

        for (ScoreEntry entry : entries) {
            if (entry.equals(target))
                return true;
        }

        return false;
    }

    @Test
    public void publicOnly() throws Throwable {
        ScoreEntry[] result = memory.search(null, EN__IT, TestData.sentence("Hello world"), 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 2, EN__IT, "Hello world 2", "Ciao mondo 2"));
    }

    @Test
    public void userOne() throws Throwable {
        ScoreEntry[] result = memory.search(owner1, EN__IT, TestData.sentence("Hello world"), 100);

        assertEquals(4, result.length);
        assertTrue(contains(result, 1, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 2, EN__IT, "Hello world 2", "Ciao mondo 2"));
        assertTrue(contains(result, 11, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 12, EN__IT, "Hello world 2", "Ciao mondo 2"));
    }

    @Test
    public void userTwo() throws Throwable {
        ScoreEntry[] result = memory.search(owner2, EN__IT, TestData.sentence("Hello world"), 100);

        assertEquals(4, result.length);
        assertTrue(contains(result, 1, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 2, EN__IT, "Hello world 2", "Ciao mondo 2"));
        assertTrue(contains(result, 21, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 22, EN__IT, "Hello world 2", "Ciao mondo 2"));
    }

    @Test
    public void expandOneWithContext() throws Throwable {
        ContextVector vector = new ContextVector.Builder()
                .add(21, .5f)
                .build();

        ScoreEntry[] result = memory.search(owner1, EN__IT, TestData.sentence("Hello world"), vector, 100);

        assertEquals(5, result.length);
        assertTrue(contains(result, 1, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 2, EN__IT, "Hello world 2", "Ciao mondo 2"));
        assertTrue(contains(result, 11, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 12, EN__IT, "Hello world 2", "Ciao mondo 2"));
        assertTrue(contains(result, 21, EN__IT, "Hello world 1", "Ciao mondo 1"));
    }

    @Test
    public void expandTwoWithContext() throws Throwable {
        ContextVector vector = new ContextVector.Builder()
                .add(21, .5f)
                .add(22, 1.f)
                .build();

        ScoreEntry[] result = memory.search(owner1, EN__IT, TestData.sentence("Hello world"), vector, 100);

        assertEquals(6, result.length);
        assertTrue(contains(result, 1, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 2, EN__IT, "Hello world 2", "Ciao mondo 2"));
        assertTrue(contains(result, 11, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 12, EN__IT, "Hello world 2", "Ciao mondo 2"));
        assertTrue(contains(result, 21, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 22, EN__IT, "Hello world 2", "Ciao mondo 2"));
    }
}
