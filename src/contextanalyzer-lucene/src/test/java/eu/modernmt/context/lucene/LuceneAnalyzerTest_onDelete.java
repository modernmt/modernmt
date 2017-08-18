package eu.modernmt.context.lucene;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static eu.modernmt.context.lucene.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by davide on 06/08/17.
 */
public class LuceneAnalyzerTest_onDelete {

    private TLuceneAnalyzer analyzer;

    public void setup(LanguagePair... languages) throws Throwable {
        this.analyzer = new TLuceneAnalyzer(languages);
    }

    @After
    public void teardown() throws Throwable {
        if (this.analyzer != null)
            this.analyzer.close();
        this.analyzer = null;
    }

    @Test
    public void monolingualDomain() throws Throwable {
        setup(EN__IT, EN__FR);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN__IT);

        analyzer.add(new Domain(1), corpus);
        analyzer.add(new Domain(2), corpus);
        analyzer.flush();

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());

        analyzer.onDelete(TestData.deletion(1));

        TLuceneAnalyzer.Entry entry = analyzer.getEntry(2, EN__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(TestData.channels(1, 0), analyzer.getLatestChannelPositions());
        assertEquals(TestData.getTerms(EN), entry.terms);
        assertEquals(TestData.getContent(EN), entry.content);
        assertNull(analyzer.getEntry(1, EN__IT));
    }

    @Test
    public void multilingualDomain() throws Throwable {
        setup(EN__IT, EN__FR);

        DummyBilingualCorpus itCorpus = TestData.corpus("dummy", EN__IT);
        DummyBilingualCorpus frCorpus = TestData.corpus("dummy", EN__FR);
        MultilingualCorpus corpus = TestData.corpus(itCorpus, frCorpus);

        analyzer.add(new Domain(1), corpus);
        analyzer.add(new Domain(2), itCorpus);
        analyzer.flush();

        assertEquals(3, analyzer.getIndexSize());
        assertEquals(3, analyzer.getStorageSize());

        analyzer.onDelete(TestData.deletion(1));

        TLuceneAnalyzer.Entry entry = analyzer.getEntry(2, EN__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(TestData.channels(1, 0), analyzer.getLatestChannelPositions());
        assertEquals(TestData.getTerms(EN), entry.terms);
        assertEquals(TestData.getContent(EN), entry.content);
        assertNull(analyzer.getEntry(1, EN__IT));
        assertNull(analyzer.getEntry(1, EN__FR));
    }

    @Test
    public void multipleDomains() throws Throwable {
        setup(EN__IT, EN__FR);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN__IT);

        analyzer.add(new Domain(1), corpus);
        analyzer.add(new Domain(2), corpus);
        analyzer.flush();

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());

        analyzer.onDelete(TestData.deletion(0, 1));
        analyzer.onDelete(TestData.deletion(1, 2));

        assertEquals(0, analyzer.getIndexSize());
        assertEquals(0, analyzer.getStorageSize());
        assertEquals(TestData.channels(1, 1), analyzer.getLatestChannelPositions());
        assertNull(analyzer.getEntry(1, EN__IT));
        assertNull(analyzer.getEntry(2, EN__IT));
    }

    @Test
    public void deleteContributions() throws Throwable {
        setup(EN__IT);
        List<TranslationUnit> units1 = TestData.tuList(0, 0, 1, EN__IT, 10);
        List<TranslationUnit> units2 = TestData.tuList(0, 10, 2, EN__IT, 10);

        analyzer.onDataReceived(units1);
        analyzer.onDataReceived(units2);
        analyzer.flush();

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());

        analyzer.onDelete(TestData.deletion(0, 1));

        TLuceneAnalyzer.Entry entry = analyzer.getEntry(2, EN__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(19, (long) analyzer.getLatestChannelPositions().get((short) 0));
        assertEquals(0, (long) analyzer.getLatestChannelPositions().get((short) 1));
        assertEquals(TestData.tuGetTerms(units2, true), entry.terms);
        assertEquals(TestData.tuGetContent(units2, true), entry.content);
    }

    @Test
    public void duplicateDelete() throws Throwable {
        setup(EN__IT);
        List<TranslationUnit> units1 = TestData.tuList(1, 0, 1, EN__IT, 10);
        List<TranslationUnit> units2 = TestData.tuList(1, 10, 2, EN__IT, 10);

        analyzer.onDataReceived(units1);
        analyzer.onDataReceived(units2);
        analyzer.flush();

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());

        analyzer.onDelete(TestData.deletion(0, 1));

        TLuceneAnalyzer.Entry entry1 = analyzer.getEntry(1, EN__IT);
        TLuceneAnalyzer.Entry entry2 = analyzer.getEntry(2, EN__IT);

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());
        assertEquals(TestData.channels(1, 19), analyzer.getLatestChannelPositions());
        assertEquals(TestData.tuGetTerms(units1, true), entry1.terms);
        assertEquals(TestData.tuGetContent(units1, true), entry1.content);
        assertEquals(TestData.tuGetTerms(units2, true), entry2.terms);
        assertEquals(TestData.tuGetContent(units2, true), entry2.content);
    }

}
