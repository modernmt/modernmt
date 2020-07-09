package eu.modernmt.decoder.neural.memory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static eu.modernmt.decoder.neural.memory.TestData.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by davide on 03/08/17.
 */
public class TestLuceneTranslationMemory_tuUpdate {

    private TLuceneTranslationMemory memory;

    @Before
    public void setup() throws Throwable {
        this.memory = new TLuceneTranslationMemory();

        memory.onDataReceived(Arrays.asList(
                TestData.tu(1L, EN__IT, "This is an example", "Questo è un esempio"),
                TestData.tu(1L, EN__FR, "This is an example", "Ceci est un exemple"),
                TestData.tu(2L, EN__IT, "This is an example", "Questo è un esempio")
        ));
    }

    @After
    public void teardown() throws Throwable {
        this.memory.close();
        this.memory = null;
    }

    @Test
    public void updateByValueExisting() throws Throwable {
        // Overwrite - 1, IT
        memory.onDataReceived(tu(0, 1L,
                1L, EN__IT, "This is an example", "Questo è un esempio 1IT",
                "This is an example", "Questo è un esempio"));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                TestData.tu(1L, EN__IT, "This is an example", "Questo è un esempio 1IT"),
                TestData.tu(1L, EN__FR, "This is an example", "Ceci est un exemple"),
                TestData.tu(2L, EN__IT, "This is an example", "Questo è un esempio")
        ), memory.entrySet());

        // Overwrite - 2, IT
        memory.onDataReceived(tu(0, 2L,
                2L, EN__IT, "This is an example", "Questo è un esempio 2IT",
                "This is an example", "Questo è un esempio"));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                TestData.tu(1L, EN__IT, "This is an example", "Questo è un esempio 1IT"),
                TestData.tu(1L, EN__FR, "This is an example", "Ceci est un exemple"),
                TestData.tu(2L, EN__IT, "This is an example", "Questo è un esempio 2IT")
        ), memory.entrySet());

        // Overwrite - 1, FR
        memory.onDataReceived(tu(0, 3L,
                1L, EN__FR, "This is an example", "Ceci est un exemple 1FR",
                "This is an example", "Ceci est un exemple"));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                TestData.tu(1L, EN__IT, "This is an example", "Questo è un esempio 1IT"),
                TestData.tu(1L, EN__FR, "This is an example", "Ceci est un exemple 1FR"),
                TestData.tu(2L, EN__IT, "This is an example", "Questo è un esempio 2IT")
        ), memory.entrySet());
    }

    @Test
    public void updateByValueNotExisting() throws Throwable {
        memory.onDataReceived(tu(0, 1L,
                1L, EN__IT, "This is a second example", "Questo è un secondo esempio",
                "This is a second example", "Questo è un esempio secondo"));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                TestData.tu(1L, EN__IT, "This is an example", "Questo è un esempio"),
                TestData.tu(1L, EN__IT, "This is a second example", "Questo è un secondo esempio"),
                TestData.tu(1L, EN__FR, "This is an example", "Ceci est un exemple"),
                TestData.tu(2L, EN__IT, "This is an example", "Questo è un esempio")
        ), memory.entrySet());
    }

}
