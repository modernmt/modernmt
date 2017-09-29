package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Memory;
import org.junit.After;
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

    public void setup(LanguagePair... languages) throws Throwable {
        this.memory = new TLuceneTranslationMemory(languages);
    }

    @After
    public void teardown() throws Throwable {
        this.memory.close();
        this.memory = null;
    }

    private void testSuccess(List<TranslationUnit> units) throws IOException {
        Set<TLuceneTranslationMemory.Entry> expectedEntries =
                TLuceneTranslationMemory.Entry.asEntrySet(memory.getLanguageIndex(), units);

        memory.add(new Memory(1), TestData.corpus("none", units));

        assertEquals(units.size(), memory.size());
        assertEquals(expectedEntries, memory.entrySet());
        assertTrue(memory.getLatestChannelPositions().isEmpty());
    }

    private void testFail(List<TranslationUnit> units) throws IOException {
        memory.add(new Memory(1), TestData.corpus("none", units));

        assertEquals(0, memory.size());
        assertTrue(memory.entrySet().isEmpty());
    }

    @Test
    public void monoDirectionalMemoryAndDirectMemory() throws Throwable {
        setup(EN__IT);
        testSuccess(TestData.tuList(EN__IT, 10));
    }

    @Test
    public void monoDirectionalMemoryAndReversedMemory() throws Throwable {
        setup(EN__IT);
        testSuccess(TestData.tuList(IT__EN, 10));
    }

    @Test
    public void monoDirectionalMemoryAndDialectMemory() throws Throwable {
        setup(EN__IT);
        testSuccess(TestData.tuList(EN_US__IT, 10));
    }

    @Test
    public void biDirectionalMemory() throws Throwable {
        setup(EN__IT, IT__EN);
        testSuccess(TestData.tuList(EN__IT, 10));
    }

    @Test
    public void biDirectionalMemoryAndDialectMemory() throws Throwable {
        setup(EN__IT, IT__EN);
        testSuccess(TestData.tuList(EN_US__IT, 10));
    }

    @Test
    public void dialectMonoDirectionalMemoryAndDirectMemory() throws Throwable {
        setup(EN_US__IT);
        testFail(TestData.tuList(EN__IT, 10));
    }

    @Test
    public void dialectMonoDirectionalMemoryAndReversedMemory() throws Throwable {
        setup(EN_US__IT);
        testFail(TestData.tuList(IT__EN, 10));
    }

    @Test
    public void dialectMonoDirectionalMemoryAndDialectMemory() throws Throwable {
        setup(EN_US__IT);
        testSuccess(TestData.tuList(EN_US__IT, 10));
    }

    @Test
    public void dialectMonoDirectionalMemoryAndDialectReversedMemory() throws Throwable {
        setup(EN_US__IT);
        testSuccess(TestData.tuList(IT__EN_US, 10));
    }

    @Test
    public void dialectBiDirectionalMemory() throws Throwable {
        setup(EN_US__IT, IT__EN_US);
        testFail(TestData.tuList(EN__IT, 10));
    }

    @Test
    public void dialectBiDirectionalMemoryAndDialectMemory() throws Throwable {
        setup(EN_US__IT, IT__EN_US);
        testSuccess(TestData.tuList(EN_US__IT, 10));
    }

    @Test
    public void multilingualMemoryAndOneDirectionMemory() throws Throwable {
        setup(EN__IT, FR__ES);
        testSuccess(TestData.tuList(EN__IT, 10));
    }

    @Test
    public void multilingualMemoryAndAllDirectionMemory() throws Throwable {
        setup(EN__IT, FR__ES);

        List<TranslationUnit> units = Arrays.asList(
                TestData.tu(0, 0L, 1L, EN__IT),
                TestData.tu(0, 1L, 1L, EN__IT),
                TestData.tu(0, 2L, 1L, FR__ES),
                TestData.tu(0, 3L, 1L, FR__ES)
        );

        testSuccess(units);
    }

}
