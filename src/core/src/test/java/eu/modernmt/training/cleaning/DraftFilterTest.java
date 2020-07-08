package eu.modernmt.training.cleaning;

import eu.modernmt.cleaning.FilteredMultilingualCorpus;
import eu.modernmt.cleaning.filters.draft.DraftFilter;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.TranslationUnit;
import eu.modernmt.training.MockMultilingualCorpus;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by davide on 14/03/16.
 */
public class DraftFilterTest {

    private static FilteredMultilingualCorpus wrap(MockMultilingualCorpus corpus) {
        return new FilteredMultilingualCorpus(corpus, null, new DraftFilter());
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
        MockMultilingualCorpus corpus = new MockMultilingualCorpus(new TranslationUnit[]{
                MockMultilingualCorpus.tu("A", "a", 0),
                MockMultilingualCorpus.tu("B", "b", 1),
                MockMultilingualCorpus.tu("C", "c", 2),
                MockMultilingualCorpus.tu("D", "d", 3),
        });

        FilteredMultilingualCorpus filteredCorpus = wrap(corpus);
        MockMultilingualCorpus output = MockMultilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(corpus, output);
    }

    @Test
    public void testDraftsSortedByDate() throws IOException {
        MockMultilingualCorpus corpus = new MockMultilingualCorpus(new TranslationUnit[]{
                MockMultilingualCorpus.tu("A", "z", 0),
                MockMultilingualCorpus.tu("A", "a", 1),
                MockMultilingualCorpus.tu("B", "z", 2),
                MockMultilingualCorpus.tu("C", "z", 3),
                MockMultilingualCorpus.tu("D", "d", 4),
                MockMultilingualCorpus.tu("C", "c", 5),
                MockMultilingualCorpus.tu("B", "b", 6),
        });

        MockMultilingualCorpus expected = new MockMultilingualCorpus(new TranslationUnit[]{
                MockMultilingualCorpus.tu("A", "a", 1),
                MockMultilingualCorpus.tu("D", "d", 4),
                MockMultilingualCorpus.tu("C", "c", 5),
                MockMultilingualCorpus.tu("B", "b", 6),
        });

        FilteredMultilingualCorpus filteredCorpus = wrap(corpus);
        MockMultilingualCorpus output = MockMultilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

    @Test
    public void testMixedDraftsWithShuffledDates() throws IOException {
        MockMultilingualCorpus corpus = new MockMultilingualCorpus(new TranslationUnit[]{
                MockMultilingualCorpus.tu("A", "z", 1),
                MockMultilingualCorpus.tu("A", "a", 0),
                MockMultilingualCorpus.tu("B", "z", 2),
                MockMultilingualCorpus.tu("C", "z", 5),
                MockMultilingualCorpus.tu("D", "d", 4),
                MockMultilingualCorpus.tu("C", "c", 3),
                MockMultilingualCorpus.tu("B", "b", 6),
        });

        MockMultilingualCorpus expected = new MockMultilingualCorpus(new TranslationUnit[]{
                MockMultilingualCorpus.tu("A", "z", 1),
                MockMultilingualCorpus.tu("C", "z", 5),
                MockMultilingualCorpus.tu("D", "d", 4),
                MockMultilingualCorpus.tu("B", "b", 6),
        });

        FilteredMultilingualCorpus filteredCorpus = wrap(corpus);
        MockMultilingualCorpus output = MockMultilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

    @Test
    public void testDraftsWithSameDate() throws IOException {
        MockMultilingualCorpus corpus = new MockMultilingualCorpus(new TranslationUnit[]{
                MockMultilingualCorpus.tu("A", "z", 0),
                MockMultilingualCorpus.tu("B", "b", 1),
                MockMultilingualCorpus.tu("A", "a", 0),
                MockMultilingualCorpus.tu("C", "c", 2),
                MockMultilingualCorpus.tu("D", "d", 3),
        });

        MockMultilingualCorpus expected = new MockMultilingualCorpus(new TranslationUnit[]{
                MockMultilingualCorpus.tu("B", "b", 1),
                MockMultilingualCorpus.tu("A", "a", 0),
                MockMultilingualCorpus.tu("C", "c", 2),
                MockMultilingualCorpus.tu("D", "d", 3),

        });

        FilteredMultilingualCorpus filteredCorpus = wrap(corpus);
        MockMultilingualCorpus output = MockMultilingualCorpus.drain(filteredCorpus.getContentReader());

        assertEquals(expected, output);
    }

}
