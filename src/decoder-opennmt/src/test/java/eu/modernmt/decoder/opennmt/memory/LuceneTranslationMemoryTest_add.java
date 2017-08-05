package eu.modernmt.decoder.opennmt.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Domain;
import eu.modernmt.test.TestData;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static eu.modernmt.test.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 03/08/17.
 */
public class LuceneTranslationMemoryTest_add {

    private InspectableLuceneTranslationMemory memory;

    public void setup(LanguagePair... languages) throws Throwable {
        this.memory = new InspectableLuceneTranslationMemory(languages);
    }

    @After
    public void teardown() throws Throwable {
        this.memory.close();
        this.memory = null;
    }

    private void testSuccess(List<TranslationUnit> units) throws IOException {
        Set<InspectableLuceneTranslationMemory.Entry> expectedEntries =
                InspectableLuceneTranslationMemory.Entry.asEntrySet(memory.getLanguages(), units);

        memory.add(new Domain(1), TestData.corpus("none", units));

        assertEquals(units.size(), memory.size());
        assertEquals(expectedEntries, memory.entrySet());
        assertTrue(memory.getLatestChannelPositions().isEmpty());
    }

    private void testFail(List<TranslationUnit> units) throws IOException {
        memory.add(new Domain(1), TestData.corpus("none", units));

        assertEquals(0, memory.size());
        assertTrue(memory.entrySet().isEmpty());
    }

    @Test
    public void monoDirectionalMemoryAndDirectDomain() throws Throwable {
        setup(EN__IT);
        testSuccess(TestData.tuList(EN__IT, 10));
    }

    @Test
    public void monoDirectionalMemoryAndReversedDomain() throws Throwable {
        setup(EN__IT);
        testSuccess(TestData.tuList(IT__EN, 10));
    }

    @Test
    public void monoDirectionalMemoryAndDialectDomain() throws Throwable {
        setup(EN__IT);
        testSuccess(TestData.tuList(EN_US__IT, 10));
    }

    @Test
    public void biDirectionalMemory() throws Throwable {
        setup(EN__IT, IT__EN);
        testSuccess(TestData.tuList(EN__IT, 10));
    }

    @Test
    public void biDirectionalMemoryAndDialectDomain() throws Throwable {
        setup(EN__IT, IT__EN);
        testSuccess(TestData.tuList(EN_US__IT, 10));
    }

    @Test
    public void dialectMonoDirectionalMemoryAndDirectDomain() throws Throwable {
        setup(EN_US__IT);
        testFail(TestData.tuList(EN__IT, 10));
    }

    @Test
    public void dialectMonoDirectionalMemoryAndReversedDomain() throws Throwable {
        setup(EN_US__IT);
        testFail(TestData.tuList(IT__EN, 10));
    }

    @Test
    public void dialectMonoDirectionalMemoryAndDialectDomain() throws Throwable {
        setup(EN_US__IT);
        testSuccess(TestData.tuList(EN_US__IT, 10));
    }

    @Test
    public void dialectMonoDirectionalMemoryAndDialectReversedDomain() throws Throwable {
        setup(EN_US__IT);
        testSuccess(TestData.tuList(IT__EN_US, 10));
    }

    @Test
    public void dialectBiDirectionalMemory() throws Throwable {
        setup(EN_US__IT, IT__EN_US);
        testFail(TestData.tuList(EN__IT, 10));
    }

    @Test
    public void dialectBiDirectionalMemoryAndDialectDomain() throws Throwable {
        setup(EN_US__IT, IT__EN_US);
        testSuccess(TestData.tuList(EN_US__IT, 10));
    }

    @Test
    public void multilingualMemoryAndOneDirectionDomain() throws Throwable {
        setup(EN__IT, FR__ES);
        testSuccess(TestData.tuList(EN__IT, 10));
    }

    @Test
    public void multilingualMemoryAndAllDirectionDomain() throws Throwable {
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
