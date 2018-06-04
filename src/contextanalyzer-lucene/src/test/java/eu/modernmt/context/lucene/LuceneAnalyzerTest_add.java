package eu.modernmt.context.lucene;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Memory;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.junit.After;
import org.junit.Test;

import static eu.modernmt.context.lucene.TestData.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by davide on 06/08/17.
 */
public class LuceneAnalyzerTest_add {

    private TLuceneAnalyzer analyzer;

    private void setup(LanguagePair... languages) throws Throwable {
        this.analyzer = new TLuceneAnalyzer(languages);
    }

    @After
    public void teardown() throws Throwable {
        if (this.analyzer != null)
            this.analyzer.close();
        this.analyzer = null;
    }

    @Test
    public void monoDirectionalAnalyzerAndDirectMemory() throws Throwable {
        setup(EN__IT);
        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN__IT);

        analyzer.add(new Memory(1), corpus);
        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(corpus.getSourceCorpus(), entry.content);
        assertEquals(TestData.getTerms(EN), entry.terms);
    }

    @Test
    public void monoDirectionalAnalyzerAndReversedMemory() throws Throwable {
        setup(EN__IT);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", IT__EN);
        analyzer.add(new Memory(1), corpus);

        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(corpus.getTargetCorpus(), entry.content);
        assertEquals(TestData.getTerms(EN), entry.terms);
    }

    @Test
    public void monoDirectionalAnalyzerAndDialectMemory() throws Throwable {
        setup(EN__IT);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN_US__IT);
        analyzer.add(new Memory(1), corpus);

        assertEquals(0, analyzer.getIndexSize());
        assertEquals(0, analyzer.getStorageSize());
    }

    @Test
    public void biDirectionalAnalyzer() throws Throwable {
        setup(EN__IT, IT__EN);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN__IT);
        analyzer.add(new Memory(1), corpus);

        TLuceneAnalyzer.Entry fwdEntry = analyzer.getEntry(1, EN__IT);
        TLuceneAnalyzer.Entry bwdEntry = analyzer.getEntry(1, IT__EN);

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());
        assertEquals(corpus.getSourceCorpus(), fwdEntry.content);
        assertEquals(TestData.getTerms(EN), fwdEntry.terms);
        assertEquals(corpus.getTargetCorpus(), bwdEntry.content);
        assertEquals(TestData.getTerms(IT), bwdEntry.terms);
    }

    @Test
    public void biDirectionalAnalyzerAndDialectMemory() throws Throwable {
        setup(EN__IT, IT__EN);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN_US__IT);
        analyzer.add(new Memory(1), corpus);

        assertEquals(0, analyzer.getIndexSize());
        assertEquals(0, analyzer.getStorageSize());
    }

    @Test
    public void multilingualAnalyzerAndOneDirectionMemory() throws Throwable {
        setup(EN__IT, EN__FR);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN__IT);
        analyzer.add(new Memory(1), corpus);

        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(corpus.getSourceCorpus(), entry.content);
        assertEquals(TestData.getTerms(EN), entry.terms);
    }

    @Test
    public void multilingualAnalyzerAndAllDirectionMemory() throws Throwable {
        setup(IT__EN, EN__FR);

        DummyBilingualCorpus itCorpus = TestData.corpus("dummy", IT__EN);
        DummyBilingualCorpus frCorpus = TestData.corpus("dummy", EN__FR);
        MultilingualCorpus corpus = TestData.corpus(itCorpus, frCorpus);

        analyzer.add(new Memory(1), corpus);

        TLuceneAnalyzer.Entry itEntry = analyzer.getEntry(1, IT__EN);
        TLuceneAnalyzer.Entry frEntry = analyzer.getEntry(1, EN__FR);

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());
        assertEquals(TestData.getContent(IT), itEntry.content);
        assertEquals(TestData.getTerms(IT), itEntry.terms);
        assertEquals(TestData.getContent(EN), frEntry.content);
        assertEquals(TestData.getTerms(EN), frEntry.terms);
    }

    @Test
    public void multilingualAnalyzerAndMultipleAllDirectionMemorys() throws Throwable {
        setup(IT__EN, EN__FR);

        DummyBilingualCorpus itCorpus = TestData.corpus("dummy", IT__EN);
        DummyBilingualCorpus frCorpus = TestData.corpus("dummy", EN__FR);

        MultilingualCorpus corpus1 = TestData.corpus(itCorpus, frCorpus, frCorpus);
        MultilingualCorpus corpus2 = TestData.corpus(itCorpus, itCorpus, frCorpus);

        analyzer.add(new Memory(1), corpus1);
        analyzer.add(new Memory(2), corpus2);

        TLuceneAnalyzer.Entry itEntry1 = analyzer.getEntry(1, IT__EN);
        TLuceneAnalyzer.Entry frEntry1 = analyzer.getEntry(1, EN__FR);
        TLuceneAnalyzer.Entry itEntry2 = analyzer.getEntry(2, IT__EN);
        TLuceneAnalyzer.Entry frEntry2 = analyzer.getEntry(2, EN__FR);

        assertEquals(4, analyzer.getIndexSize());
        assertEquals(4, analyzer.getStorageSize());

        assertEquals(TestData.getContent(IT), itEntry1.content);
        assertEquals(TestData.getTerms(IT), itEntry1.terms);
        assertEquals(TestData.getContent(EN, EN), frEntry1.content);
        assertEquals(TestData.getTerms(EN), frEntry1.terms);

        assertEquals(TestData.getContent(IT, IT), itEntry2.content);
        assertEquals(TestData.getTerms(IT), itEntry2.terms);
        assertEquals(TestData.getContent(EN), frEntry2.content);
        assertEquals(TestData.getTerms(EN), frEntry2.terms);
    }

}
