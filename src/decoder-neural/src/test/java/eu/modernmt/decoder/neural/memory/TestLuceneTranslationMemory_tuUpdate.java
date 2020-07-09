package eu.modernmt.decoder.neural.memory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

        memory.onDataReceived(
                addition(0, 0, 1L, tu(EN__IT, "This is an example", "Questo è un esempio")),
                addition(0, 1, 1L, tu(EN__FR, "This is an example", "Ceci est un exemple")),
                addition(0, 2, 2L, tu(EN__IT, "This is an example", "Questo è un esempio"))
        );
    }

    @After
    public void teardown() throws Throwable {
        this.memory.close();
        this.memory = null;
    }

    @Test
    public void updateByValueExisting() throws Throwable {
        // Overwrite - 1, IT
        memory.onDataReceived(overwrite(0, 3,
                1L, tu(EN__IT, "This is an example", "Questo è un esempio 1IT"),
                "This is an example", "Questo è un esempio"));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                addition(0, 0, 1L, tu(EN__IT, "This is an example", "Questo è un esempio 1IT")),
                addition(0, 0, 1L, tu(EN__FR, "This is an example", "Ceci est un exemple")),
                addition(0, 0, 2L, tu(EN__IT, "This is an example", "Questo è un esempio"))
        ), memory.entrySet());

        // Overwrite - 2, IT
        memory.onDataReceived(overwrite(0, 4,
                2L, tu(EN__IT, "This is an example", "Questo è un esempio 2IT"),
                "This is an example", "Questo è un esempio"));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                addition(0, 0, 1L, tu(EN__IT, "This is an example", "Questo è un esempio 1IT")),
                addition(0, 0, 1L, tu(EN__FR, "This is an example", "Ceci est un exemple")),
                addition(0, 0, 2L, tu(EN__IT, "This is an example", "Questo è un esempio 2IT"))
        ), memory.entrySet());

        // Overwrite - 1, FR
        memory.onDataReceived(overwrite(0, 5,
                1L, tu(EN__FR, "This is an example", "Ceci est un exemple 1FR"),
                "This is an example", "Ceci est un exemple"));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                addition(0, 0, 1L, tu(EN__IT, "This is an example", "Questo è un esempio 1IT")),
                addition(0, 0, 1L, tu(EN__FR, "This is an example", "Ceci est un exemple 1FR")),
                addition(0, 0, 2L, tu(EN__IT, "This is an example", "Questo è un esempio 2IT"))
        ), memory.entrySet());
    }

    @Test
    public void updateByValueNotExisting() throws Throwable {
        memory.onDataReceived(overwrite(0, 3,
                1L, tu(EN__IT, "This is a second example", "Questo è un secondo esempio"),
                "This is a second example", "Questo è un esempio secondo"));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                addition(0, 0, 1L, tu(EN__IT, "This is an example", "Questo è un esempio")),
                addition(0, 0, 1L, tu(EN__IT, "This is a second example", "Questo è un secondo esempio")),
                addition(0, 0, 1L, tu(EN__FR, "This is an example", "Ceci est un exemple")),
                addition(0, 0, 2L, tu(EN__IT, "This is an example", "Questo è un esempio"))
        ), memory.entrySet());
    }

}
