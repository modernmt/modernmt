package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.DataManager;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Memory;
import eu.modernmt.model.corpus.MultilingualCorpus;
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

        MultilingualCorpus corpus1 = TestData.corpus("none",
                Collections.singletonList(TestData.tu(EN__IT, "Hello world 1", "Ciao mondo 1", null)));
        MultilingualCorpus corpus2 = TestData.corpus("none",
                Collections.singletonList(TestData.tu(EN__IT, "Hello world 2", "Ciao mondo 2", null)));

        memory.bulkInsert(new Memory(1), corpus1);
        memory.bulkInsert(new Memory(2), corpus2);
        memory.bulkInsert(new Memory(11, owner1, "none"), corpus1);
        memory.bulkInsert(new Memory(12, owner1, "none"), corpus2);
        memory.bulkInsert(new Memory(21, owner2, "none"), corpus1);
        memory.bulkInsert(new Memory(22, owner2, "none"), corpus2);
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
