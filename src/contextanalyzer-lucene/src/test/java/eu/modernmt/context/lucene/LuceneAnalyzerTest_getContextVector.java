package eu.modernmt.context.lucene;

import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static eu.modernmt.context.lucene.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 07/08/17.
 */
public class LuceneAnalyzerTest_getContextVector {

    private TLuceneAnalyzer analyzer;

    @Before
    public void setup() throws Throwable {
        this.analyzer = new TLuceneAnalyzer(EN__IT, IT__EN, EN__FR);

        String enHelloWorld = "hello world 1\nhello world 2";
        String enTheTest = "the test 1\nthe test 2";
        String itHelloWorld = "ciao mondo 1\nciao mondo 2";
        String itTheTest = "il test 1\nil test 2";
        String frHelloWorld = "bonjour monde 1\nbonjour monde 2";
        String frTheTest = "le preuve 1\nle preuve 2";

        DummyBilingualCorpus enItHelloWorld = TestData.corpus("none", EN__IT, enHelloWorld, itHelloWorld);
        DummyBilingualCorpus enFrHelloWorld = TestData.corpus("none", EN__FR, enHelloWorld, frHelloWorld);
        DummyBilingualCorpus enItTheTest = TestData.corpus("none", EN__IT, enTheTest, itTheTest);
        DummyBilingualCorpus enFrTheTest = TestData.corpus("none", EN__FR, enTheTest, frTheTest);

        MultilingualCorpus langMixedCorpus = TestData.corpus(enItHelloWorld, enFrHelloWorld);
        MultilingualCorpus contentMixedCorpus = TestData.corpus(enItHelloWorld, enItTheTest);

        this.analyzer.add(new Domain(1), enItHelloWorld);
        this.analyzer.add(new Domain(2), enFrHelloWorld);
        this.analyzer.add(new Domain(3), enItTheTest);
        this.analyzer.add(new Domain(4), enFrTheTest);

        this.analyzer.add(new Domain(12), langMixedCorpus);
        this.analyzer.add(new Domain(13), contentMixedCorpus);

        this.analyzer.flush();
    }

    @After
    public void teardown() throws Throwable {
        if (this.analyzer != null)
            this.analyzer.close();
        this.analyzer = null;
    }

    private static boolean contains(ContextVector result, long domain) {
        for (ContextVector.Entry entry : result) {
            if (entry.domain.getId() == domain)
                return true;
        }

        return false;
    }

    @Test
    public void directSearchWithItalianHelloWorld() throws Throwable {
        ContextVector result = analyzer.getContextVector(EN__IT, "hello world", 100);

        assertEquals(3, result.size());
        assertTrue(contains(result, 1));
        assertTrue(contains(result, 12));
        assertTrue(contains(result, 13));
    }

    @Test
    public void directSearchWithItalianTheTest() throws Throwable {
        ContextVector result = analyzer.getContextVector(EN__IT, "the test", 100);

        assertEquals(2, result.size());
        assertTrue(contains(result, 3));
        assertTrue(contains(result, 13));
    }

    @Test
    public void directSearchWithFrenchHelloWorld() throws Throwable {
        ContextVector result = analyzer.getContextVector(EN__FR, "hello world", 100);

        assertEquals(2, result.size());
        assertTrue(contains(result, 2));
        assertTrue(contains(result, 12));
    }

    @Test
    public void reversedSearchWithItalianHelloWorld() throws Throwable {
        ContextVector result = analyzer.getContextVector(IT__EN, "ciao mondo", 100);

        assertEquals(3, result.size());
        assertTrue(contains(result, 1));
        assertTrue(contains(result, 12));
        assertTrue(contains(result, 13));
    }

    @Test
    public void reversedSearchWithFrenchHelloWorld() throws Throwable {
        ContextVector result = analyzer.getContextVector(FR__EN, "bonjour monde", 100);
        assertEquals(0, result.size());
    }

}
