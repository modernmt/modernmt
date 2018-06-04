package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.model.Memory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static eu.modernmt.decoder.neural.memory.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 03/08/17.
 */
public class LuceneTranslationMemoryTest_add {

    private TLuceneTranslationMemory memory;

    @Before
    public void setup() throws Throwable {
        this.memory = new TLuceneTranslationMemory();
    }

    @After
    public void teardown() throws Throwable {
        this.memory.close();
        this.memory = null;
    }

    private void test(List<TranslationUnit> units) throws IOException {
        Set<ScoreEntry> expectedEntries = TLuceneTranslationMemory.asEntrySet(units);

        memory.bulkInsert(new Memory(1), TestData.corpus("none", units));

        assertEquals(units.size(), memory.size());
        assertEquals(expectedEntries, memory.entrySet());
        assertTrue(memory.getLatestChannelPositions().isEmpty());
    }

    @Test
    public void directMemory() throws Throwable {
        test(TestData.tuList(EN__IT, 10));
    }

    @Test
    public void reversedMemory() throws Throwable {
        test(TestData.tuList(IT__EN, 10));
    }

    @Test
    public void directDialectMemory() throws Throwable {
        test(TestData.tuList(EN_US__IT, 10));
    }

    @Test
    public void reversedDialectMemory() throws Throwable {
        test(TestData.tuList(IT__EN_US, 10));
    }

    @Test
    public void allDirectionsMemory() throws Throwable {
        List<TranslationUnit> units = Arrays.asList(
                TestData.tu(0, 0L, 1L, EN__IT, null),
                TestData.tu(0, 1L, 1L, EN__IT, null),
                TestData.tu(0, 2L, 1L, FR__ES, null),
                TestData.tu(0, 3L, 1L, FR__ES, null)
        );

        test(units);
    }

}
