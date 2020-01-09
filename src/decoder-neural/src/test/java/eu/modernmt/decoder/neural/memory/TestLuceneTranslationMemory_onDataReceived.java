package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.memory.TranslationMemory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static eu.modernmt.decoder.neural.memory.TestData.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by davide on 03/08/17.
 */
public class TestLuceneTranslationMemory_onDataReceived {

    private TLuceneTranslationMemory memory;

    @Before
    public void setup() throws Throwable {
        this.memory = new TLuceneTranslationMemory();
    }

    @After
    public void teardown() throws IOException {
        this.memory.close();
        this.memory = null;
    }

    private void test(List<TranslationUnit> units) throws IOException {
        int size = units.size();
        Map<Short, Long> expectedChannels = TestData.channels(0, size - 1);
        Set<TranslationMemory.Entry> expectedEntries = TLuceneTranslationMemory.asEntrySet(units);

        memory.onDataReceived(units);

        assertEquals(size + 1, memory.size());
        assertEquals(expectedEntries, memory.entrySet());
        assertEquals(expectedChannels, memory.getLatestChannelPositions());
    }

    @Test
    public void directContributions() throws Throwable {
        test(TestData.tuList(EN__IT, 4));
    }

    @Test
    public void reversedContributions() throws Throwable {
        test(TestData.tuList(IT__EN, 4));
    }

    @Test
    public void allDirectionsContributions() throws Throwable {
        List<TranslationUnit> units = Arrays.asList(
                TestData.tu(0, 0L, 1L, EN__IT, null),
                TestData.tu(0, 1L, 2L, EN__IT, null),
                TestData.tu(0, 2L, 1L, FR__ES, null),
                TestData.tu(0, 3L, 1L, FR__ES, null)
        );

        test(units);
    }

    @Test
    public void duplicateContribution() throws Throwable {
        List<TranslationUnit> units = Arrays.asList(
                TestData.tu(1, 0L, 1L, EN__IT, null),
                TestData.tu(1, 1L, 1L, EN__IT, null)
        );

        List<TranslationUnit> cloneUnits = Arrays.asList(
                TestData.tu(1, 0L, 2L, FR__ES, null),
                TestData.tu(1, 1L, 2L, FR__ES, null)
        );

        Map<Short, Long> expectedChannels = TestData.channels(1, 1);
        memory.onDataReceived(units);

        assertEquals(2 + 1, memory.size());
        assertEquals(expectedChannels, memory.getLatestChannelPositions());

        memory.onDataReceived(cloneUnits);

        assertEquals(2 + 1, memory.size());
        assertEquals(expectedChannels, memory.getLatestChannelPositions());
    }

}
