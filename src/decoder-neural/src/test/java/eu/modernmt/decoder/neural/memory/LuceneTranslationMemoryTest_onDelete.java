package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Memory;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static eu.modernmt.decoder.neural.memory.TestData.EN__IT;
import static eu.modernmt.decoder.neural.memory.TestData.FR__ES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 03/08/17.
 */
public class LuceneTranslationMemoryTest_onDelete {

    private TLuceneTranslationMemory memory;

    public void setup(LanguagePair... languages) throws Throwable {
        this.memory = new TLuceneTranslationMemory(languages);
    }

    @After
    public void teardown() throws Throwable {
        this.memory.close();
        this.memory = null;
    }

    @Test
    public void monolingualMemory() throws Throwable {
        setup(EN__IT, FR__ES);
        List<TranslationUnit> units1 = TestData.tuList(1, EN__IT, 10);
        List<TranslationUnit> units2 = TestData.tuList(2, EN__IT, 10);

        Set<ScoreEntry> expectedEntries = TLuceneTranslationMemory.asEntrySet(memory.getLanguages(), units2);

        memory.bulkInsert(new Memory(1), TestData.corpus("none", units1));
        memory.bulkInsert(new Memory(2), TestData.corpus("none", units2));
        assertEquals(20, memory.size());

        memory.onDelete(TestData.deletion(1));

        assertEquals(10 + 1, memory.size());
        assertEquals(TestData.channels(1, 0), memory.getLatestChannelPositions());
        assertEquals(expectedEntries, memory.entrySet());
    }

    @Test
    public void multilingualMemory() throws Throwable {
        setup(EN__IT, FR__ES);

        List<TranslationUnit> units1 = Arrays.asList(
                TestData.tu(1L, EN__IT, null),
                TestData.tu(1L, EN__IT, null),
                TestData.tu(1L, FR__ES, null),
                TestData.tu(1L, FR__ES, null)
        );
        List<TranslationUnit> units2 = TestData.tuList(2, EN__IT, 10);

        Set<ScoreEntry> expectedEntries = TLuceneTranslationMemory.asEntrySet(memory.getLanguages(), units2);

        memory.bulkInsert(new Memory(1), TestData.corpus("none", units1));
        memory.bulkInsert(new Memory(2), TestData.corpus("none", units2));
        assertEquals(14, memory.size());

        memory.onDelete(TestData.deletion(1));

        assertEquals(10 + 1, memory.size());
        assertEquals(TestData.channels(1, 0), memory.getLatestChannelPositions());
        assertEquals(expectedEntries, memory.entrySet());
    }

    @Test
    public void multipleMemories() throws Throwable {
        setup(EN__IT, FR__ES);
        List<TranslationUnit> units1 = TestData.tuList(1, EN__IT, 10);
        List<TranslationUnit> units2 = TestData.tuList(2, EN__IT, 10);

        memory.bulkInsert(new Memory(1), TestData.corpus("none", units1));
        memory.bulkInsert(new Memory(2), TestData.corpus("none", units2));
        assertEquals(20, memory.size());

        memory.onDelete(TestData.deletion(0, 1));
        memory.onDelete(TestData.deletion(1, 2));

        assertEquals(1, memory.size());
        assertEquals(TestData.channels(1, 1), memory.getLatestChannelPositions());
        assertTrue(memory.entrySet().isEmpty());
    }

    @Test
    public void deleteContributions() throws Throwable {
        setup(EN__IT);
        List<TranslationUnit> units1 = TestData.tuList(0, 0, 1, EN__IT, 10);
        List<TranslationUnit> units2 = TestData.tuList(0, 10, 2, EN__IT, 10);

        Set<ScoreEntry> expectedEntries = TLuceneTranslationMemory.asEntrySet(memory.getLanguages(), units2);

        memory.onDataReceived(units1);
        memory.onDataReceived(units2);
        assertEquals(20 + 1, memory.size());

        memory.onDelete(TestData.deletion(0, 1));

        assertEquals(10 + 1, memory.size());
        assertEquals(19, (long) memory.getLatestChannelPositions().get((short) 0));
        assertEquals(0, (long) memory.getLatestChannelPositions().get((short) 1));
        assertEquals(expectedEntries, memory.entrySet());
    }

    @Test
    public void duplicateDelete() throws Throwable {
        setup(EN__IT);
        List<TranslationUnit> units1 = TestData.tuList(1, 0, 1, EN__IT, 10);
        List<TranslationUnit> units2 = TestData.tuList(1, 10, 2, EN__IT, 10);

        Set<ScoreEntry> expectedEntries = new HashSet<>();
        expectedEntries.addAll(TLuceneTranslationMemory.asEntrySet(memory.getLanguages(), units1));
        expectedEntries.addAll(TLuceneTranslationMemory.asEntrySet(memory.getLanguages(), units2));

        memory.onDataReceived(units1);
        memory.onDataReceived(units2);
        assertEquals(20 + 1, memory.size());

        memory.onDelete(TestData.deletion(0, 1));

        assertEquals(20 + 1, memory.size());
        assertEquals(TestData.channels(1, 19), memory.getLatestChannelPositions());
        assertEquals(expectedEntries, memory.entrySet());
    }

}
