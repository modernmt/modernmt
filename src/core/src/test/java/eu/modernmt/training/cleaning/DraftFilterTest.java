package eu.modernmt.training.cleaning;

import eu.modernmt.cleaning.FilterEngine;
import eu.modernmt.cleaning.FilteredMultilingualCorpus;
import eu.modernmt.cleaning.filters.draft.DraftFilter;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.training.MockMultilingualCorpus;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by davide on 14/03/16.
 */
public class DraftFilterTest {

    private static FilteredMultilingualCorpus wrap(MockMultilingualCorpus corpus) {
        FilterEngine.Builder builder = new FilterEngine.Builder();
        builder.add(new DraftFilter());

        return new FilteredMultilingualCorpus(corpus, builder.build());
    }

    @Test
    public void testAllUniqueWithoutDate() throws IOException {
        MockMultilingualCorpus corpus = new MockMultilingualCorpus(new String[][]{
                {"A", "a"}, {"B", "b"}, {"C", "c"}, {"D", "d"},
        });

        FilteredMultilingualCorpus filteredCorpus = wrap(corpus);
        MockMultilingualCorpus output = MockMultilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(corpus, output);
    }

    @Test
    public void testSequentialDraftsWithoutDate() throws IOException {
        MockMultilingualCorpus corpus = new MockMultilingualCorpus(new String[][]{
                {"A", "z"}, {"A", "a"}, {"B", "b"}, {"C", "c"}, {"D", "d"},
        });
        MockMultilingualCorpus expected = new MockMultilingualCorpus(new String[][]{
                {"A", "a"}, {"B", "b"}, {"C", "c"}, {"D", "d"},
        });

        FilteredMultilingualCorpus filteredCorpus = wrap(corpus);
        MockMultilingualCorpus output = MockMultilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

    @Test
    public void testMixedDraftsWithoutDate() throws IOException {
        MockMultilingualCorpus corpus = new MockMultilingualCorpus(new String[][]{
                {"A", "z"}, {"A", "a"}, {"B", "z"}, {"C", "z"}, {"D", "d"}, {"C", "c"}, {"B", "b"},
        });
        MockMultilingualCorpus expected = new MockMultilingualCorpus(new String[][]{
                {"A", "a"}, {"D", "d"}, {"C", "c"}, {"B", "b"}
        });

        FilteredMultilingualCorpus filteredCorpus = wrap(corpus);
        MockMultilingualCorpus output = MockMultilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

    @Test
    public void testAllUniqueSortedByDate() throws IOException {
        MockMultilingualCorpus corpus = new MockMultilingualCorpus(new MultilingualCorpus.StringPair[]{
                MockMultilingualCorpus.pair("A", "a", 0),
                MockMultilingualCorpus.pair("B", "b", 1),
                MockMultilingualCorpus.pair("C", "c", 2),
                MockMultilingualCorpus.pair("D", "d", 3),
        });

        FilteredMultilingualCorpus filteredCorpus = wrap(corpus);
        MockMultilingualCorpus output = MockMultilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(corpus, output);
    }

    @Test
    public void testDraftsSortedByDate() throws IOException {
        MockMultilingualCorpus corpus = new MockMultilingualCorpus(new MultilingualCorpus.StringPair[]{
                MockMultilingualCorpus.pair("A", "z", 0),
                MockMultilingualCorpus.pair("A", "a", 1),
                MockMultilingualCorpus.pair("B", "z", 2),
                MockMultilingualCorpus.pair("C", "z", 3),
                MockMultilingualCorpus.pair("D", "d", 4),
                MockMultilingualCorpus.pair("C", "c", 5),
                MockMultilingualCorpus.pair("B", "b", 6),
        });

        MockMultilingualCorpus expected = new MockMultilingualCorpus(new MultilingualCorpus.StringPair[]{
                MockMultilingualCorpus.pair("A", "a", 1),
                MockMultilingualCorpus.pair("D", "d", 4),
                MockMultilingualCorpus.pair("C", "c", 5),
                MockMultilingualCorpus.pair("B", "b", 6),
        });

        FilteredMultilingualCorpus filteredCorpus = wrap(corpus);
        MockMultilingualCorpus output = MockMultilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

    @Test
    public void testMixedDraftsWithShuffledDates() throws IOException {
        MockMultilingualCorpus corpus = new MockMultilingualCorpus(new MultilingualCorpus.StringPair[]{
                MockMultilingualCorpus.pair("A", "z", 1),
                MockMultilingualCorpus.pair("A", "a", 0),
                MockMultilingualCorpus.pair("B", "z", 2),
                MockMultilingualCorpus.pair("C", "z", 5),
                MockMultilingualCorpus.pair("D", "d", 4),
                MockMultilingualCorpus.pair("C", "c", 3),
                MockMultilingualCorpus.pair("B", "b", 6),
        });

        MockMultilingualCorpus expected = new MockMultilingualCorpus(new MultilingualCorpus.StringPair[]{
                MockMultilingualCorpus.pair("A", "z", 1),
                MockMultilingualCorpus.pair("C", "z", 5),
                MockMultilingualCorpus.pair("D", "d", 4),
                MockMultilingualCorpus.pair("B", "b", 6),
        });

        FilteredMultilingualCorpus filteredCorpus = wrap(corpus);
        MockMultilingualCorpus output = MockMultilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

    @Test
    public void testDraftsWithSameDate() throws IOException {
        MockMultilingualCorpus corpus = new MockMultilingualCorpus(new MultilingualCorpus.StringPair[]{
                MockMultilingualCorpus.pair("A", "z", 0),
                MockMultilingualCorpus.pair("B", "b", 1),
                MockMultilingualCorpus.pair("A", "a", 0),
                MockMultilingualCorpus.pair("C", "c", 2),
                MockMultilingualCorpus.pair("D", "d", 3),
        });

        MockMultilingualCorpus expected = new MockMultilingualCorpus(new MultilingualCorpus.StringPair[]{
                MockMultilingualCorpus.pair("B", "b", 1),
                MockMultilingualCorpus.pair("A", "a", 0),
                MockMultilingualCorpus.pair("C", "c", 2),
                MockMultilingualCorpus.pair("D", "d", 3),

        });

        FilteredMultilingualCorpus filteredCorpus = wrap(corpus);
        MockMultilingualCorpus output = MockMultilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

}
