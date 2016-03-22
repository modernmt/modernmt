package eu.modernmt.engine.training.cleaning;

import eu.modernmt.engine.training.mock.MockBilingualCorpus;
import eu.modernmt.model.BilingualCorpus;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by davide on 14/03/16.
 */
public class DraftFilterTest {

    @Test
    public void testAllUniqueWithoutDate() throws IOException {
        MockBilingualCorpus corpus = new MockBilingualCorpus(new String[][]{
                {"A", "a"}, {"B", "b"}, {"C", "c"}, {"D", "d"},
        });

        FilteredBilingualCorpus filteredCorpus = new FilteredBilingualCorpus(corpus);
        filteredCorpus.addFilter(new DraftFilter());

        MockBilingualCorpus output = MockBilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(corpus, output);
    }

    @Test
    public void testSequentialDraftsWithoutDate() throws IOException {
        MockBilingualCorpus corpus = new MockBilingualCorpus(new String[][]{
                {"A", "z"}, {"A", "a"}, {"B", "b"}, {"C", "c"}, {"D", "d"},
        });
        MockBilingualCorpus expected = new MockBilingualCorpus(new String[][]{
                {"A", "a"}, {"B", "b"}, {"C", "c"}, {"D", "d"},
        });

        FilteredBilingualCorpus filteredCorpus = new FilteredBilingualCorpus(corpus);
        filteredCorpus.addFilter(new DraftFilter());

        MockBilingualCorpus output = MockBilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

    @Test
    public void testMixedDraftsWithoutDate() throws IOException {
        MockBilingualCorpus corpus = new MockBilingualCorpus(new String[][]{
                {"A", "z"}, {"A", "a"}, {"B", "z"}, {"C", "z"}, {"D", "d"}, {"C", "c"}, {"B", "b"},
        });
        MockBilingualCorpus expected = new MockBilingualCorpus(new String[][]{
                {"A", "a"}, {"D", "d"}, {"C", "c"}, {"B", "b"}
        });

        FilteredBilingualCorpus filteredCorpus = new FilteredBilingualCorpus(corpus);
        filteredCorpus.addFilter(new DraftFilter());

        MockBilingualCorpus output = MockBilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

    @Test
    public void testAllUniqueSortedByDate() throws IOException {
        MockBilingualCorpus corpus = new MockBilingualCorpus(new BilingualCorpus.StringPair[]{
                MockBilingualCorpus.pair("A", "a", 0),
                MockBilingualCorpus.pair("B", "b", 1),
                MockBilingualCorpus.pair("C", "c", 2),
                MockBilingualCorpus.pair("D", "d", 3),
        });

        FilteredBilingualCorpus filteredCorpus = new FilteredBilingualCorpus(corpus);
        filteredCorpus.addFilter(new DraftFilter());

        MockBilingualCorpus output = MockBilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(corpus, output);
    }

    @Test
    public void testDraftsSortedByDate() throws IOException {
        MockBilingualCorpus corpus = new MockBilingualCorpus(new BilingualCorpus.StringPair[]{
                MockBilingualCorpus.pair("A", "z", 0),
                MockBilingualCorpus.pair("A", "a", 1),
                MockBilingualCorpus.pair("B", "z", 2),
                MockBilingualCorpus.pair("C", "z", 3),
                MockBilingualCorpus.pair("D", "d", 4),
                MockBilingualCorpus.pair("C", "c", 5),
                MockBilingualCorpus.pair("B", "b", 6),
        });

        MockBilingualCorpus expected = new MockBilingualCorpus(new BilingualCorpus.StringPair[]{
                MockBilingualCorpus.pair("A", "a", 1),
                MockBilingualCorpus.pair("D", "d", 4),
                MockBilingualCorpus.pair("C", "c", 5),
                MockBilingualCorpus.pair("B", "b", 6),
        });

        FilteredBilingualCorpus filteredCorpus = new FilteredBilingualCorpus(corpus);
        filteredCorpus.addFilter(new DraftFilter());

        MockBilingualCorpus output = MockBilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

    @Test
    public void testMixedDraftsWithShuffledDates() throws IOException {
        MockBilingualCorpus corpus = new MockBilingualCorpus(new BilingualCorpus.StringPair[]{
                MockBilingualCorpus.pair("A", "z", 1),
                MockBilingualCorpus.pair("A", "a", 0),
                MockBilingualCorpus.pair("B", "z", 2),
                MockBilingualCorpus.pair("C", "z", 5),
                MockBilingualCorpus.pair("D", "d", 4),
                MockBilingualCorpus.pair("C", "c", 3),
                MockBilingualCorpus.pair("B", "b", 6),
        });

        MockBilingualCorpus expected = new MockBilingualCorpus(new BilingualCorpus.StringPair[]{
                MockBilingualCorpus.pair("A", "z", 1),
                MockBilingualCorpus.pair("C", "z", 5),
                MockBilingualCorpus.pair("D", "d", 4),
                MockBilingualCorpus.pair("B", "b", 6),
        });

        FilteredBilingualCorpus filteredCorpus = new FilteredBilingualCorpus(corpus);
        filteredCorpus.addFilter(new DraftFilter());

        MockBilingualCorpus output = MockBilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

    @Test
    public void testDraftsWithSameDate() throws IOException {
        MockBilingualCorpus corpus = new MockBilingualCorpus(new BilingualCorpus.StringPair[]{
                MockBilingualCorpus.pair("A", "z", 0),
                MockBilingualCorpus.pair("B", "b", 1),
                MockBilingualCorpus.pair("A", "a", 0),
                MockBilingualCorpus.pair("C", "c", 2),
                MockBilingualCorpus.pair("D", "d", 3),
        });

        MockBilingualCorpus expected = new MockBilingualCorpus(new BilingualCorpus.StringPair[]{
                MockBilingualCorpus.pair("B", "b", 1),
                MockBilingualCorpus.pair("A", "a", 0),
                MockBilingualCorpus.pair("C", "c", 2),
                MockBilingualCorpus.pair("D", "d", 3),

        });

        FilteredBilingualCorpus filteredCorpus = new FilteredBilingualCorpus(corpus);
        filteredCorpus.addFilter(new DraftFilter());

        MockBilingualCorpus output = MockBilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

}
