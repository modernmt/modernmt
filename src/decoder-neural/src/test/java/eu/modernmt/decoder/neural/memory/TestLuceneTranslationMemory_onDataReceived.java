package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.TranslationUnitMessage;
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

    private void test(TranslationUnitMessage... units) throws IOException {
        test(Arrays.asList(units));
    }

    private void test(List<TranslationUnitMessage> units) throws IOException {
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
        test(additions(EN__IT, 4));
    }

    @Test
    public void reversedContributions() throws Throwable {
        test(additions(IT__EN, 4));
    }

    @Test
    public void allDirectionsContributions() throws Throwable {
        test(
                addition(0, 0L, 1L, tu(EN__IT)),
                addition(0, 1L, 2L, tu(EN__IT)),
                addition(0, 2L, 1L, tu(FR__ES)),
                addition(0, 3L, 1L, tu(FR__ES))
        );
    }

    @Test
    public void duplicateContribution() throws Throwable {
        List<TranslationUnitMessage> units = Arrays.asList(
                addition(1, 0L, 1L, tu(EN__IT)),
                addition(1, 1L, 1L, tu(EN__IT))
        );

        List<TranslationUnitMessage> cloneUnits = Arrays.asList(
                addition(1, 0L, 2L, tu(FR__ES)),
                addition(1, 1L, 2L, tu(FR__ES))
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
