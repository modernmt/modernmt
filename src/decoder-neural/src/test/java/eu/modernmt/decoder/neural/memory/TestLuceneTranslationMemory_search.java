package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.TranslationUnitMessage;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.ContextVector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static eu.modernmt.decoder.neural.memory.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 03/08/17.
 */
public class TestLuceneTranslationMemory_search {

    private TLuceneTranslationMemory memory;
    private final ContextVector context = ContextVector.fromString("1:1,2:1");

    @Before
    public void setup() throws Throwable {
        this.memory = new TLuceneTranslationMemory();

        List<TranslationUnitMessage> units1 = Arrays.asList(
                addition(0, 0, 1, tu(EN__IT, "Hello world 1", "Ciao mondo 1")),
                addition(0, 1, 1, tu(EN_US__IT_CH, "The test 1", "Il test 1")),
                addition(0, 2, 1, tu(EN__FR, "Hello world 1", "Bonjour monde 1")),
                addition(0, 3, 1, tu(EN__FR, "The test 1", "Le preuve 1"))
        );

        List<TranslationUnitMessage> units2 = Arrays.asList(
                addition(0, 4, 2, tu(IT__EN, "Ciao mondo 2", "Hello world 2")),
                addition(0, 5, 2, tu(IT_CH__EN_US, "Il test 2", "The test 2")),
                addition(0, 6, 2, tu(FR__EN, "Bonjour monde 2", "Hello world 2")),
                addition(0, 7, 2, tu(FR__EN, "Le preuve 2", "The test 2"))
        );

        this.memory.onDataReceived(units1);
        this.memory.onDataReceived(units2);
    }

    @After
    public void teardown() throws Throwable {
        this.memory.close();
        this.memory = null;
    }

    private static boolean contains(ScoreEntry[] entries, long memory, LanguageDirection direction, String sentence, String translation) {
        ScoreEntry target = new ScoreEntry(memory, direction, sentence.split(" "), translation.split(" "));

        for (ScoreEntry entry : entries) {
            if (entry.equals(target))
                return true;
        }

        return false;
    }

    @Test
    public void directSearchWithItalianHelloWorld() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, EN__IT, TestData.sentence("Hello world"), context, 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 2, EN__IT, "Hello world 2", "Ciao mondo 2"));
    }

    @Test
    public void directSearchWithItalianTheTest() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, EN__IT, TestData.sentence("The test"), context, 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN_US__IT_CH, "The test 1", "Il test 1"));
        assertTrue(contains(result, 2, EN_US__IT_CH, "The test 2", "Il test 2"));
    }

    @Test
    public void directSearchWithItalianDialect() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, EN_US__IT_CH, TestData.sentence("Hello world"), context, 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 2, EN__IT, "Hello world 2", "Ciao mondo 2"));
    }

    @Test
    public void reversedSearchWithItalianHelloWorld() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, IT__EN, TestData.sentence("Ciao mondo"), context, 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, IT__EN, "Ciao mondo 1", "Hello world 1"));
        assertTrue(contains(result, 2, IT__EN, "Ciao mondo 2", "Hello world 2"));
    }

    @Test
    public void reversedSearchWithItalianTheTest() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, IT__EN, TestData.sentence("Il test"), context, 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, IT_CH__EN_US, "Il test 1", "The test 1"));
        assertTrue(contains(result, 2, IT_CH__EN_US, "Il test 2", "The test 2"));
    }

    @Test
    public void reversedSearchWithItalianDialect() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, IT_CH__EN_US, TestData.sentence("Ciao mondo"), context, 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, IT__EN, "Ciao mondo 1", "Hello world 1"));
        assertTrue(contains(result, 2, IT__EN, "Ciao mondo 2", "Hello world 2"));
    }

    @Test
    public void directSearchWithFrenchHelloWorld() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, EN__FR, TestData.sentence("Hello world"), context, 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN__FR, "Hello world 1", "Bonjour monde 1"));
        assertTrue(contains(result, 2, EN__FR, "Hello world 2", "Bonjour monde 2"));
    }

    @Test
    public void directSearchWithFrenchDialect() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, EN_US__FR_CA, TestData.sentence("Hello world"), context, 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN__FR, "Hello world 1", "Bonjour monde 1"));
        assertTrue(contains(result, 2, EN__FR, "Hello world 2", "Bonjour monde 2"));
    }

    @Test
    public void reversedSearchWithFrenchHelloWorld() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, FR__EN, TestData.sentence("Bonjour monde"), context, 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, FR__EN, "Bonjour monde 1", "Hello world 1"));
        assertTrue(contains(result, 2, FR__EN, "Bonjour monde 2", "Hello world 2"));
    }

    @Test
    public void reversedSearchWithFrenchDialect() throws Throwable {
        ScoreEntry[] result = this.memory.search(null, FR_CA__EN_US, TestData.sentence("Bonjour monde"), context, 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, FR__EN, "Bonjour monde 1", "Hello world 1"));
        assertTrue(contains(result, 2, FR__EN, "Bonjour monde 2", "Hello world 2"));
    }

}
