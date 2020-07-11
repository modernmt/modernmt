package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.TranslationUnitMessage;
import eu.modernmt.memory.TranslationMemory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static eu.modernmt.decoder.neural.memory.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 03/08/17.
 */
public class TestLuceneTranslationMemory_onDelete {

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

    @Test
    public void monolingualMemory() throws Throwable {
        List<TranslationUnitMessage> units1 = additions(0, 0, 1, EN__IT, 10);
        List<TranslationUnitMessage> units2 = additions(0, 10, 2, EN__IT, 10);

        Set<TranslationMemory.Entry> expectedEntries = TLuceneTranslationMemory.asEntrySet(units2);

        memory.onDataReceived(units1);
        memory.onDataReceived(units2);
        assertEquals(20 + 1, memory.size());

        memory.onDelete(TestData.deletion(1));

        assertEquals(10 + 1, memory.size());
        assertEquals(TestData.channels(19L, 0L), memory.getLatestChannelPositions());
        assertEquals(expectedEntries, memory.entrySet());
    }

    @Test
    public void multilingualMemory() throws Throwable {
        List<TranslationUnitMessage> units1 = Arrays.asList(
                addition(0, 0, 1L, tu(EN__IT)),
                addition(0, 1, 1L, tu(EN__IT)),
                addition(0, 2, 1L, tu(FR__ES)),
                addition(0, 3, 1L, tu(FR__ES))
        );
        List<TranslationUnitMessage> units2 = additions(0, 4, 2, EN__IT, 10);

        Set<TranslationMemory.Entry> expectedEntries = TLuceneTranslationMemory.asEntrySet(units2);

        memory.onDataReceived(units1);
        memory.onDataReceived(units2);
        assertEquals(14 + 1, memory.size());

        memory.onDelete(TestData.deletion(1));

        assertEquals(10 + 1, memory.size());
        assertEquals(TestData.channels(13L, 0L), memory.getLatestChannelPositions());
        assertEquals(expectedEntries, memory.entrySet());
    }

    @Test
    public void multipleMemories() throws Throwable {
        List<TranslationUnitMessage> units1 = additions(0, 0, 1, EN__IT, 10);
        List<TranslationUnitMessage> units2 = additions(0, 10, 2, EN__IT, 10);

        memory.onDataReceived(units1);
        memory.onDataReceived(units2);
        assertEquals(20 + 1, memory.size());

        memory.onDelete(TestData.deletion(0, 1));
        memory.onDelete(TestData.deletion(1, 2));

        assertEquals(1, memory.size());
        assertEquals(TestData.channels(19L, 1L), memory.getLatestChannelPositions());
        assertTrue(memory.entrySet().isEmpty());
    }

    @Test
    public void deleteContributions() throws Throwable {
        List<TranslationUnitMessage> units1 = additions(0, 0, 1, EN__IT, 10);
        List<TranslationUnitMessage> units2 = additions(0, 10, 2, EN__IT, 10);

        Set<TranslationMemory.Entry> expectedEntries = TLuceneTranslationMemory.asEntrySet(units2);

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
        List<TranslationUnitMessage> units1 = additions(1, 0, 1, EN__IT, 10);
        List<TranslationUnitMessage> units2 = additions(1, 10, 2, EN__IT, 10);

        Set<TranslationMemory.Entry> expectedEntries = new HashSet<>();
        expectedEntries.addAll(TLuceneTranslationMemory.asEntrySet(units1));
        expectedEntries.addAll(TLuceneTranslationMemory.asEntrySet(units2));

        memory.onDataReceived(units1);
        memory.onDataReceived(units2);
        assertEquals(20 + 1, memory.size());

        memory.onDelete(TestData.deletion(0, 1));

        assertEquals(20 + 1, memory.size());
        assertEquals(TestData.channels(1, 19), memory.getLatestChannelPositions());
        assertEquals(expectedEntries, memory.entrySet());
    }

}
