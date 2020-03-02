package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Memory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static eu.modernmt.context.lucene.TestData.DummyBilingualCorpus;
import static eu.modernmt.context.lucene.TestData.EN__IT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by davide on 07/08/17.
 */
public class TestLuceneAnalyzer_getContextVectorWithOwners {

    private static final UUID owner1 = new UUID(0, 1);
    private static final UUID owner2 = new UUID(0, 2);

    private TLuceneAnalyzer analyzer;

    @Before
    public void setup() throws Throwable {
        this.analyzer = new TLuceneAnalyzer();

        DummyBilingualCorpus corpus1 = TestData.corpus("none", EN__IT, "hello world 1", "ciao mondo 1");
        DummyBilingualCorpus corpus2 = TestData.corpus("none", EN__IT, "hello world 2", "ciao mondo 2");

        this.analyzer.onDataReceived(new Memory(1), corpus1);
        this.analyzer.onDataReceived(new Memory(2), corpus2);
        this.analyzer.onDataReceived(new Memory(11, owner1, "none"), corpus1);
        this.analyzer.onDataReceived(new Memory(12, owner1, "none"), corpus2);
        this.analyzer.onDataReceived(new Memory(21, owner2, "none"), corpus1);
        this.analyzer.onDataReceived(new Memory(22, owner2, "none"), corpus2);
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

    private void test(UUID owner, int... memories) throws ContextAnalyzerException {
        ContextVector result = analyzer.getContextVector(owner, EN__IT, "hello world", 100);

        assertEquals(memories == null ? 0 : memories.length, result.size());
        if (memories != null) {
            for (int memory : memories)
                assertTrue(contains(result, memory));
        }
    }

    @Test
    public void publicOnly() throws Throwable {
        test(null, 1, 2);
    }

    @Test
    public void userOne() throws Throwable {
        test(owner1, 1, 2, 11, 12);
    }

    @Test
    public void userTwo() throws Throwable {
        test(owner2, 1, 2, 21, 22);
    }

}
