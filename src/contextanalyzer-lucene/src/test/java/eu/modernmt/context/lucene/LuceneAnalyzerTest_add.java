package eu.modernmt.context.lucene;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.junit.After;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static eu.modernmt.context.lucene.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    public void test() throws Throwable {

    }

}
