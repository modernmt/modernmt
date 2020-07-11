package eu.modernmt.decoder.neural.memory;

import eu.modernmt.model.corpus.TranslationUnit;
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

    private static final TranslationUnit enItTu = tu("id-0001", EN__IT, "This is an example", "Questo è un esempio");
    private static final TranslationUnit enFrTu = tu("id-0002", EN__FR, "This is an example", "Ceci est un exemple");

    @Before
    public void setup() throws Throwable {
        this.memory = new TLuceneTranslationMemory();

        memory.onDataReceived(
                addition(0, 0, 1L, enItTu),
                addition(0, 1, 1L, enFrTu),
                addition(0, 2, 2L, enItTu)
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
                addition(0, 0, 1L, enFrTu),
                addition(0, 0, 2L, enItTu)
        ), memory.entrySet());

        // Overwrite - 2, IT
        memory.onDataReceived(overwrite(0, 4,
                2L, tu(EN__IT, "This is an example", "Questo è un esempio 2IT"),
                "This is an example", "Questo è un esempio"));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                addition(0, 0, 1L, tu(EN__IT, "This is an example", "Questo è un esempio 1IT")),
                addition(0, 0, 1L, enFrTu),
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
                addition(0, 0, 1L, enItTu),
                addition(0, 0, 1L, tu(EN__IT, "This is a second example", "Questo è un secondo esempio")),
                addition(0, 0, 1L, enFrTu),
                addition(0, 0, 2L, enItTu)
        ), memory.entrySet());
    }

    @Test
    public void updateByTuidExisting() throws Throwable {
        // Overwrite - 1, IT
        memory.onDataReceived(overwrite(0, 3,
                1L, tu("id-0001", EN__IT, "This is an example 1EN", "Questo è un esempio 1IT")));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                addition(0, 0, 1L, tu("id-0001", EN__IT, "This is an example 1EN", "Questo è un esempio 1IT")),
                addition(0, 0, 1L, enFrTu),
                addition(0, 0, 2L, enItTu)
        ), memory.entrySet());

        // Overwrite - 2, IT
        memory.onDataReceived(overwrite(0, 4,
                2L, tu("id-0001", EN__IT, "This is an example 2EN", "Questo è un esempio 2IT")));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                addition(0, 0, 1L, tu("id-0001", EN__IT, "This is an example 1EN", "Questo è un esempio 1IT")),
                addition(0, 0, 1L, enFrTu),
                addition(0, 0, 2L, tu("id-0001", EN__IT, "This is an example 2EN", "Questo è un esempio 2IT"))
        ), memory.entrySet());

        // Overwrite - 1, FR
        memory.onDataReceived(overwrite(0, 5,
                1L, tu("id-0002", EN__FR, "This is an example 1EN", "Ceci est un exemple 1FR")));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                addition(0, 0, 1L, tu("id-0001", EN__IT, "This is an example 1EN", "Questo è un esempio 1IT")),
                addition(0, 0, 1L, tu("id-0002", EN__FR, "This is an example 1EN", "Ceci est un exemple 1FR")),
                addition(0, 0, 2L, tu("id-0001", EN__IT, "This is an example 2EN", "Questo è un esempio 2IT"))
        ), memory.entrySet());
    }

    @Test
    public void updateByTuidNotExisting() throws Throwable {
        memory.onDataReceived(overwrite(0, 3,
                1L, tu("id-0010", EN__IT, "This is an example", "Questo è un esempio")));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                addition(0, 0, 1L, enItTu),
                addition(0, 0, 1L, tu("id-0010", EN__IT, "This is an example", "Questo è un esempio")),
                addition(0, 0, 1L, enFrTu),
                addition(0, 0, 2L, enItTu)
        ), memory.entrySet());
    }

    @Test
    public void updateByDuplicateTuidDifferentLanguage() throws Throwable {
        memory.onDataReceived(overwrite(0, 3,
                1L, tu("id-0001", EN__FR, "This is an example", "Ceci est un exemple")));

        assertEquals(TLuceneTranslationMemory.asEntrySet(
                addition(0, 0, 1L, enItTu),
                addition(0, 0, 1L, tu("id-0001", EN__FR, "This is an example", "Ceci est un exemple")),
                addition(0, 0, 1L, enFrTu),
                addition(0, 0, 2L, enItTu)
        ), memory.entrySet());
    }

}
