package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.data.DataManager;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Memory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static eu.modernmt.context.lucene.TestData.DummyBilingualCorpus;
import static eu.modernmt.context.lucene.TestData.EN__IT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 07/08/17.
 */
public class LuceneAnalyzerTest_getContextVectorWithOwners {

    private TLuceneAnalyzer analyzer;

    @Before
    public void setup() throws Throwable {
        this.analyzer = new TLuceneAnalyzer();

        DummyBilingualCorpus corpus1 = TestData.corpus("none", EN__IT, "hello world 1", "ciao mondo 1");
        DummyBilingualCorpus corpus2 = TestData.corpus("none", EN__IT, "hello world 2", "ciao mondo 2");

        this.analyzer.add(new Memory(1), corpus1);
        this.analyzer.add(new Memory(2), corpus2);
        this.analyzer.add(new Memory(11, 1, "none"), corpus1);
        this.analyzer.add(new Memory(12, 1, "none"), corpus2);
        this.analyzer.add(new Memory(21, 2, "none"), corpus1);
        this.analyzer.add(new Memory(22, 2, "none"), corpus2);

        this.analyzer.flush();
    }

    @After
    public void teardown() throws Throwable {
        if (this.analyzer != null)
            this.analyzer.close();
        this.analyzer = null;
    }

    private static boolean contains(ContextVector result, long memory) {
        for (ContextVector.Entry entry : result) {
            if (entry.memory.getId() == memory)
                return true;
        }

        return false;
    }

    private void test(long owner, int... memories) throws ContextAnalyzerException {
        ContextVector result = analyzer.getContextVector(owner, EN__IT, "hello world", 100);

        assertEquals(memories == null ? 0 : memories.length, result.size());
        if (memories != null) {
            for (int memory : memories)
                assertTrue(contains(result, memory));
        }
    }

    @Test
    public void publicOnly() throws Throwable {
        test(DataManager.PUBLIC, 1, 2);
    }

    @Test
    public void userOne() throws Throwable {
        test(1, 1, 2, 11, 12);
    }

    @Test
    public void userTwo() throws Throwable {
        test(2, 1, 2, 21, 22);
    }

}
