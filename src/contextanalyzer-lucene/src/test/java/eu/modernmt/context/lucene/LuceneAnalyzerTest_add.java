package eu.modernmt.context.lucene;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.lucene.index.IndexNotFoundException;
import org.junit.After;
import org.junit.Test;

import static eu.modernmt.context.lucene.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by davide on 06/08/17.
 */
public class LuceneAnalyzerTest_add {

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
    public void monoDirectionalAnalyzerAndDirectDomain() throws Throwable {
        setup(EN__IT);
        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN__IT);

        analyzer.add(new Domain(1), corpus);
        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(corpus.getSourceCorpus(), entry.content);
        assertEquals(TestData.getTerms(EN), entry.terms);
    }

    @Test
    public void monoDirectionalAnalyzerAndReversedDomain() throws Throwable {
        setup(EN__IT);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", IT__EN);
        analyzer.add(new Domain(1), corpus);

        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(corpus.getTargetCorpus(), entry.content);
        assertEquals(TestData.getTerms(EN), entry.terms);
    }

    @Test
    public void monoDirectionalAnalyzerAndDialectDomain() throws Throwable {
        setup(EN__IT);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN_US__IT);
        analyzer.add(new Domain(1), corpus);

        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(corpus.getSourceCorpus(), entry.content);
        assertEquals(TestData.getTerms(EN), entry.terms);
    }

    @Test
    public void biDirectionalAnalyzer() throws Throwable {
        setup(EN__IT, IT__EN);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN__IT);
        analyzer.add(new Domain(1), corpus);

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
    public void biDirectionalAnalyzerAndDialectDomain() throws Throwable {
        setup(EN__IT, IT__EN);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN_US__IT);
        analyzer.add(new Domain(1), corpus);

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
    public void dialectMonoDirectionalAnalyzerAndDirectDomain() throws Throwable {
        setup(EN_US__IT);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN__IT);
        analyzer.add(new Domain(1), corpus);

        assertEquals(0, analyzer.getStorageSize());

        try {
            analyzer.getIndex().getIndexReader();
            fail("Expected IndexNotFoundException but not thrown");
        } catch (IndexNotFoundException e) {
            // Success
        }
    }

    @Test
    public void dialectMonoDirectionalAnalyzerAndReversedDomain() throws Throwable {
        setup(EN_US__IT);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", IT__EN);
        analyzer.add(new Domain(1), corpus);

        assertEquals(0, analyzer.getStorageSize());

        try {
            analyzer.getIndex().getIndexReader();
            fail("Expected IndexNotFoundException but not thrown");
        } catch (IndexNotFoundException e) {
            // Success
        }
    }

    @Test
    public void dialectMonoDirectionalAnalyzerAndDialectDomain() throws Throwable {
        setup(EN_US__IT);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN_US__IT);
        analyzer.add(new Domain(1), corpus);

        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN_US__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(corpus.getSourceCorpus(), entry.content);
        assertEquals(TestData.getTerms(EN), entry.terms);
    }

    @Test
    public void dialectMonoDirectionalAnalyzerAndDialectReversedDomain() throws Throwable {
        setup(EN_US__IT);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", IT__EN_US);
        analyzer.add(new Domain(1), corpus);

        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN_US__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(corpus.getTargetCorpus(), entry.content);
        assertEquals(TestData.getTerms(EN), entry.terms);
    }

    @Test
    public void dialectBiDirectionalAnalyzer() throws Throwable {
        setup(EN_US__IT, IT__EN_US);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN__IT);
        analyzer.add(new Domain(1), corpus);

        assertEquals(0, analyzer.getStorageSize());

        try {
            analyzer.getIndex().getIndexReader();
            fail("Expected IndexNotFoundException but not thrown");
        } catch (IndexNotFoundException e) {
            // Success
        }
    }

    @Test
    public void dialectBiDirectionalAnalyzerAndDialectDomain() throws Throwable {
        setup(EN_US__IT, IT__EN_US);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN_US__IT);
        analyzer.add(new Domain(1), corpus);

        TLuceneAnalyzer.Entry fwdEntry = analyzer.getEntry(1, EN_US__IT);
        TLuceneAnalyzer.Entry bwdEntry = analyzer.getEntry(1, IT__EN_US);

        assertEquals(2, analyzer.getIndexSize());
        assertEquals(2, analyzer.getStorageSize());
        assertEquals(corpus.getSourceCorpus(), fwdEntry.content);
        assertEquals(TestData.getTerms(EN), fwdEntry.terms);
        assertEquals(corpus.getTargetCorpus(), bwdEntry.content);
        assertEquals(TestData.getTerms(IT), bwdEntry.terms);
    }

    @Test
    public void multilingualAnalyzerAndOneDirectionDomain() throws Throwable {
        setup(EN__IT, EN__FR);

        DummyBilingualCorpus corpus = TestData.corpus("dummy", EN__IT);
        analyzer.add(new Domain(1), corpus);

        TLuceneAnalyzer.Entry entry = analyzer.getEntry(1, EN__IT);

        assertEquals(1, analyzer.getIndexSize());
        assertEquals(1, analyzer.getStorageSize());
        assertEquals(corpus.getSourceCorpus(), entry.content);
        assertEquals(TestData.getTerms(EN), entry.terms);
    }

    @Test
    public void multilingualAnalyzerAndAllDirectionDomain() throws Throwable {
        setup(IT__EN, EN__FR);

        DummyBilingualCorpus itCorpus = TestData.corpus("dummy", IT__EN);
        DummyBilingualCorpus frCorpus = TestData.corpus("dummy", EN__FR);
        MultilingualCorpus corpus = TestData.corpus(itCorpus, frCorpus);

        analyzer.add(new Domain(1), corpus);

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
    public void multilingualAnalyzerAndMultipleAllDirectionDomains() throws Throwable {
        setup(IT__EN, EN__FR);

        DummyBilingualCorpus itCorpus = TestData.corpus("dummy", IT__EN);
        DummyBilingualCorpus frCorpus = TestData.corpus("dummy", EN__FR);

        MultilingualCorpus corpus1 = TestData.corpus(itCorpus, frCorpus, frCorpus);
        MultilingualCorpus corpus2 = TestData.corpus(itCorpus, itCorpus, frCorpus);

        analyzer.add(new Domain(1), corpus1);
        analyzer.add(new Domain(2), corpus2);

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
