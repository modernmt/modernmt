package eu.modernmt.context.lucene;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.model.Memory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static eu.modernmt.context.lucene.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by davide on 06/08/17.
 */
public class TestLuceneAnalyzer_onDataReceived {

    private TLuceneAnalyzer analyzer;

    @Before
    public void setup() throws Throwable {
        this.analyzer = new TLuceneAnalyzer();
    }

    @After
    public void teardown() throws Throwable {
        if (this.analyzer != null)
            this.analyzer.close();
        this.analyzer = null;
    }

    private void test(List<TranslationUnit> units, boolean reversed) throws Throwable {
        analyzer.onDataReceived(units);

        Map<Short, Long> expectedChannels = TestData.channels(0, units.size() - 1);
        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN__IT);

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());
        assertEquals(expectedChannels, analyzer.getLatestChannelPositions());
        assertEquals(TestData.tuGetTerms(units, !reversed), entry.terms);
        assertEquals(TestData.tuGetContent(units, !reversed), entry.content);
    }

    @Test
    public void directMemory() throws Throwable {
        test(TestData.tuList(EN__IT, 4), false);
    }

    @Test
    public void reversedMemory() throws Throwable {
        test(TestData.tuList(IT__EN, 4), true);
    }

    @Test
    public void directDialectMemory() throws Throwable {
        test(TestData.tuList(EN_US__IT, 4), false);
    }

    @Test
    public void reversedDialectMemory() throws Throwable {
        test(TestData.tuList(IT__EN_US, 4), true);
    }

    @Test
    public void directMixedMemories() throws Throwable {
        List<TranslationUnit> units1 = Arrays.asList(
                TestData.tu(0, 0L, 1L, IT__EN, null),
                TestData.tu(0, 1L, 1L, IT__EN, null),
                TestData.tu(0, 2L, 1L, EN__FR, null),
                TestData.tu(0, 3L, 1L, EN__FR, null)
        );

        List<TranslationUnit> units2 = Arrays.asList(
                TestData.tu(0, 4L, 2L, IT__EN, null),
                TestData.tu(0, 5L, 2L, EN__FR, null)
        );

        List<TranslationUnit> allUnits = new ArrayList<>();
        allUnits.addAll(units1);
        allUnits.addAll(units2);

        analyzer.onDataReceived(allUnits);

        Map<Short, Long> expectedChannels = TestData.channels(0, 5);
        TLuceneAnalyzer.Entry itEntry1 = analyzer.getEntry(1, IT__EN);
        TLuceneAnalyzer.Entry frEntry1 = analyzer.getEntry(1, EN__FR);
        TLuceneAnalyzer.Entry itEntry2 = analyzer.getEntry(2, IT__EN);
        TLuceneAnalyzer.Entry frEntry2 = analyzer.getEntry(2, EN__FR);

        assertEquals(8, analyzer.getIndexSize());
        assertEquals(8, analyzer.getStorageSize());
        assertEquals(expectedChannels, analyzer.getLatestChannelPositions());

        assertEquals(TestData.tuGetTerms(units1, true, IT__EN), itEntry1.terms);
        assertEquals(TestData.tuGetContent(units1, true, IT__EN), itEntry1.content);
        assertEquals(TestData.tuGetTerms(units1, true, EN__FR), frEntry1.terms);
        assertEquals(TestData.tuGetContent(units1, true, EN__FR), frEntry1.content);

        assertEquals(TestData.tuGetTerms(units2, true, IT__EN), itEntry2.terms);
        assertEquals(TestData.tuGetContent(units2, true, IT__EN), itEntry2.content);
        assertEquals(TestData.tuGetTerms(units2, true, EN__FR), frEntry2.terms);
        assertEquals(TestData.tuGetContent(units2, true, EN__FR), frEntry2.content);
    }

    @Test
    public void appendToExistingMemory() throws Throwable {
        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN__IT);
        analyzer.onDataReceived(new Memory(1), corpus);

        List<TranslationUnit> units = TestData.tuList(1, 0L, 1L, EN__IT, 4);
        analyzer.onDataReceived(units);

        HashSet<String> expectedTerms = new HashSet<>();
        expectedTerms.addAll(TestData.tuGetTerms(units, true));
        expectedTerms.addAll(TestData.getTerms(EN));

        String expectedContent = corpus.getSourceCorpus() + '\n' + TestData.tuGetContent(units, true);

        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN__IT);

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());
        assertEquals(TestData.channels(10L, 3L), analyzer.getLatestChannelPositions());
        assertEquals(expectedTerms, entry.terms);
        assertEquals(expectedContent, entry.content);
    }

    @Test
    public void duplicateContribution() throws Throwable {
        List<TranslationUnit> units = Arrays.asList(
                TestData.tu(1, 0L, 1L, EN__IT, null),
                TestData.tu(1, 1L, 1L, EN__IT, null)
        );

        List<TranslationUnit> cloneUnits = Arrays.asList(
                TestData.tu(1, 0L, 2L, EN__FR, null),
                TestData.tu(1, 1L, 2L, EN__FR, null)
        );

        analyzer.onDataReceived(units);

        Map<Short, Long> expectedChannels = TestData.channels(1, 1);
        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN__IT);

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());
        assertEquals(expectedChannels, analyzer.getLatestChannelPositions());
        assertEquals(TestData.tuGetTerms(units, true), entry.terms);
        assertEquals(TestData.tuGetContent(units, true), entry.content);

        analyzer.onDataReceived(cloneUnits);

        entry = analyzer.getEntry(1, EN__IT);

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());
        assertEquals(expectedChannels, analyzer.getLatestChannelPositions());
        assertEquals(TestData.tuGetTerms(units, true), entry.terms);
        assertEquals(TestData.tuGetContent(units, true), entry.content);
        assertNull(analyzer.getEntry(2, EN__FR));
    }

}
