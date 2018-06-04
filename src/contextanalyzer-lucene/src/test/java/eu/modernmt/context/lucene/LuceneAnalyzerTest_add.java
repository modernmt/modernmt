package eu.modernmt.context.lucene;

import eu.modernmt.model.Memory;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static eu.modernmt.context.lucene.TestData.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by davide on 06/08/17.
 */
public class LuceneAnalyzerTest_add {

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

    private void test(DummyBilingualCorpus corpus, boolean reversed) throws Throwable {
        analyzer.add(new Memory(1), corpus);

        TLuceneAnalyzer.Entry fwdEntry = analyzer.getEntry(1, EN__IT);
        TLuceneAnalyzer.Entry bwdEntry = analyzer.getEntry(1, IT__EN);

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());
        assertEquals(reversed ? corpus.getTargetCorpus() : corpus.getSourceCorpus(), fwdEntry.content);
        assertEquals(TestData.getTerms(EN), fwdEntry.terms);
        assertEquals(reversed ? corpus.getSourceCorpus() : corpus.getTargetCorpus(), bwdEntry.content);
        assertEquals(TestData.getTerms(IT), bwdEntry.terms);
    }

    @Test
    public void directMemory() throws Throwable {
        test(TestData.corpus("dummy", EN__IT), false);
    }

    @Test
    public void reversedMemory() throws Throwable {
        test(TestData.corpus("dummy", IT__EN), true);
    }

    @Test
    public void directDialectMemory() throws Throwable {
        test(TestData.corpus("dummy", EN_US__IT), false);
    }

    @Test
    public void reversedDialectMemory() throws Throwable {
        test(TestData.corpus("dummy", IT__EN_US), true);
    }

    @Test
    public void directMixedMemories() throws Throwable {
        DummyBilingualCorpus itCorpus = TestData.corpus("dummy", IT__EN);
        DummyBilingualCorpus frCorpus = TestData.corpus("dummy", EN__FR);

        MultilingualCorpus corpus1 = TestData.corpus(itCorpus, frCorpus, frCorpus);
        MultilingualCorpus corpus2 = TestData.corpus(itCorpus, itCorpus, frCorpus);

        analyzer.add(new Memory(1), corpus1);
        analyzer.add(new Memory(2), corpus2);

        TLuceneAnalyzer.Entry itenEntry1 = analyzer.getEntry(1, IT__EN);
        TLuceneAnalyzer.Entry enitEntry1 = analyzer.getEntry(1, EN__IT);
        TLuceneAnalyzer.Entry frenEntry1 = analyzer.getEntry(1, FR__EN);
        TLuceneAnalyzer.Entry enfrEntry1 = analyzer.getEntry(1, EN__FR);
        TLuceneAnalyzer.Entry itenEntry2 = analyzer.getEntry(2, IT__EN);
        TLuceneAnalyzer.Entry enitEntry2 = analyzer.getEntry(2, EN__IT);
        TLuceneAnalyzer.Entry frenEntry2 = analyzer.getEntry(2, FR__EN);
        TLuceneAnalyzer.Entry enfrEntry2 = analyzer.getEntry(2, EN__FR);

        assertEquals(8, analyzer.getIndexSize());
        assertEquals(8, analyzer.getStorageSize());

        assertEquals(TestData.getContent(IT), itenEntry1.content);
        assertEquals(TestData.getTerms(IT), itenEntry1.terms);
        assertEquals(TestData.getContent(EN), enitEntry1.content);
        assertEquals(TestData.getTerms(EN), enitEntry1.terms);
        assertEquals(TestData.getContent(FR, FR), frenEntry1.content);
        assertEquals(TestData.getContent(EN, EN), enfrEntry1.content);
        assertEquals(TestData.getTerms(EN), enfrEntry1.terms);

        assertEquals(TestData.getContent(IT,IT), itenEntry2.content);
        assertEquals(TestData.getTerms(IT), itenEntry2.terms);
        assertEquals(TestData.getContent(EN, EN), enitEntry2.content);
        assertEquals(TestData.getTerms(EN), enitEntry2.terms);
        assertEquals(TestData.getContent(FR), frenEntry2.content);
        assertEquals(TestData.getContent(EN), enfrEntry2.content);
        assertEquals(TestData.getTerms(EN), enfrEntry2.terms);
    }

}
