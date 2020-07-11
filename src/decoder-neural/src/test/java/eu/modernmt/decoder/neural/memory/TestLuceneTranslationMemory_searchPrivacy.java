package eu.modernmt.decoder.neural.memory;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.ContextVector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static eu.modernmt.decoder.neural.memory.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 03/08/17.
 */
public class TestLuceneTranslationMemory_searchPrivacy {

    private TLuceneTranslationMemory memory;

    @Before
    public void setup() throws Throwable {
        this.memory = new TLuceneTranslationMemory();

        memory.onDataReceived(addition(0, 0L, 1L, tu(EN__IT, "Hello world 1", "Ciao mondo 1")));
        memory.onDataReceived(addition(0, 1L, 2L, tu(EN__IT, "Hello world 2", "Ciao mondo 2")));
    }

    @After
    public void teardown() throws IOException {
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
    public void emptyContext() throws Throwable {
        ContextVector context = ContextVector.fromString("100:1");

        ScoreEntry[] result = memory.search(null, EN__IT, TestData.sentence("Hello world"), context, 100);
        assertEquals(0, result.length);
    }

    @Test
    public void contextOne() throws Throwable {
        ContextVector context = ContextVector.fromString("1:1");

        ScoreEntry[] result = memory.search(null, EN__IT, TestData.sentence("Hello world"), context, 100);

        assertEquals(1, result.length);
        assertTrue(contains(result, 1, EN__IT, "Hello world 1", "Ciao mondo 1"));
    }

    @Test
    public void contextTwo() throws Throwable {
        ContextVector context = ContextVector.fromString("2:1");

        ScoreEntry[] result = memory.search(null, EN__IT, TestData.sentence("Hello world"), context, 100);

        assertEquals(1, result.length);
        assertTrue(contains(result, 2, EN__IT, "Hello world 2", "Ciao mondo 2"));
    }

    @Test
    public void fullContext() throws Throwable {
        ContextVector context = ContextVector.fromString("1:1,2:1");

        ScoreEntry[] result = memory.search(null, EN__IT, TestData.sentence("Hello world"), context, 100);

        assertEquals(2, result.length);
        assertTrue(contains(result, 1, EN__IT, "Hello world 1", "Ciao mondo 1"));
        assertTrue(contains(result, 2, EN__IT, "Hello world 2", "Ciao mondo 2"));
    }

}
