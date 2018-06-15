package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.DataManager;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Memory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static eu.modernmt.decoder.neural.memory.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 03/08/17.
 */
public class LuceneTranslationMemoryTest_search {

    private TLuceneTranslationMemory memory;

    @Before
    public void setup() throws Throwable {
        this.memory = new TLuceneTranslationMemory();

        ArrayList<TranslationUnit> units1 = new ArrayList<>();
        units1.add(TestData.tu(EN__IT, "Hello world 1", "Ciao mondo 1", null));
        units1.add(TestData.tu(EN_US__IT_CH, "The test 1", "Il test 1", null));
        units1.add(TestData.tu(EN__FR, "Hello world 1", "Bonjour monde 1", null));
        units1.add(TestData.tu(EN__FR, "The test 1", "Le preuve 1", null));

        ArrayList<TranslationUnit> units2 = new ArrayList<>();
        units2.add(TestData.tu(IT__EN, "Ciao mondo 2", "Hello world 2", null));
        units2.add(TestData.tu(IT_CH__EN_US, "Il test 2", "The test 2", null));
        units2.add(TestData.tu(FR__EN, "Bonjour monde 2", "Hello world 2", null));
        units2.add(TestData.tu(FR__EN, "Le preuve 2", "The test 2", null));

        this.memory.bulkInsert(new Memory(1), TestData.corpus("memory-1", units1));
        this.memory.bulkInsert(new Memory(2), TestData.corpus("memory-2", units2));
    }

    @After
    public void teardown() throws Throwable {
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
    public void directSearchWithItalianHelloWorld() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, EN__IT, TestData.sentence("Hello world"), 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 2, EN__IT, "Hello world 2", "Ciao mondo 2"));
    }

    @Test
    public void directSearchWithItalianTheTest() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, EN__IT, TestData.sentence("The test"), 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN_US__IT_CH, "The test 1", "Il test 1"));
        assertTrue(contains(result, 2, EN_US__IT_CH, "The test 2", "Il test 2"));
    }

    @Test
    public void directSearchWithItalianDialect() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, EN_US__IT_CH, TestData.sentence("Hello world"), 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 2, EN__IT, "Hello world 2", "Ciao mondo 2"));
    }

    @Test
    public void reversedSearchWithItalianHelloWorld() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, IT__EN, TestData.sentence("Ciao mondo"), 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, IT__EN, "Ciao mondo 1", "Hello world 1"));
        assertTrue(contains(result, 2, IT__EN, "Ciao mondo 2", "Hello world 2"));
    }

    @Test
    public void reversedSearchWithItalianTheTest() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, IT__EN, TestData.sentence("Il test"), 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, IT_CH__EN_US, "Il test 1", "The test 1"));
        assertTrue(contains(result, 2, IT_CH__EN_US, "Il test 2", "The test 2"));
    }

    @Test
    public void reversedSearchWithItalianDialect() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, IT_CH__EN_US, TestData.sentence("Ciao mondo"), 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, IT__EN, "Ciao mondo 1", "Hello world 1"));
        assertTrue(contains(result, 2, IT__EN, "Ciao mondo 2", "Hello world 2"));
    }

    @Test
    public void directSearchWithFrenchHelloWorld() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, EN__FR, TestData.sentence("Hello world"), 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN__FR, "Hello world 1", "Bonjour monde 1"));
        assertTrue(contains(result, 2, EN__FR, "Hello world 2", "Bonjour monde 2"));
    }

    @Test
    public void directSearchWithFrenchDialect() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, EN_US__FR_CA, TestData.sentence("Hello world"), 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN__FR, "Hello world 1", "Bonjour monde 1"));
        assertTrue(contains(result, 2, EN__FR, "Hello world 2", "Bonjour monde 2"));
    }

    @Test
    public void reversedSearchWithFrenchHelloWorld() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, FR__EN, TestData.sentence("Bonjour monde"), 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, FR__EN, "Bonjour monde 1", "Hello world 1"));
        assertTrue(contains(result, 2, FR__EN, "Bonjour monde 2", "Hello world 2"));
    }

    @Test
    public void reversedSearchWithFrenchDialect() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, FR_CA__EN_US, TestData.sentence("Bonjour monde"), 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, FR__EN, "Bonjour monde 1", "Hello world 1"));
        assertTrue(contains(result, 2, FR__EN, "Bonjour monde 2", "Hello world 2"));
    }

}
