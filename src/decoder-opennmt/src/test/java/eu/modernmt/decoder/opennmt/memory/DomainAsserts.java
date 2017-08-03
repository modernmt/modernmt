package eu.modernmt.decoder.opennmt.memory;

import eu.modernmt.decoder.opennmt.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.lang.LanguagePair;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by davide on 03/08/17.
 */
public class DomainAsserts {

    private static IndexReader getIndexReader(LuceneTranslationMemory memory) {
        try {
            Method method = LuceneTranslationMemory.class.getDeclaredMethod("getIndexReader");
            method.setAccessible(true);
            return (IndexReader) method.invoke(memory);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertSize(LuceneTranslationMemory memory, int size) {
        IndexReader reader = getIndexReader(memory);
        assertEquals(size, reader.numDocs());
    }

    public static void assertContains(LuceneTranslationMemory memory, LanguagePair language, String source, String target) throws IOException {
        ScoreEntry[] entries = memory.search(language, TestUtils.sentence(source), 1);
        assertEquals(1, entries.length);

        assertArrayEquals(source.split("\\s+"), entries[0].sentence);
        assertArrayEquals(target.split("\\s+"), entries[0].translation);
    }

    public static void assertContainsBothDirections(LuceneTranslationMemory memory, LanguagePair language, String source, String target) throws IOException {
        assertContains(memory, language, source, target);
        assertContains(memory, language.reversed(), target, source);
    }

    public static void assertNotContains(LuceneTranslationMemory memory, LanguagePair language, String source, String target) throws IOException {
        ScoreEntry[] entries = memory.search(language, TestUtils.sentence(source), 1);

        if (entries.length > 0) {
            boolean sameSource = Arrays.equals(source.split("\\s+"), entries[0].sentence);
            boolean sameTarget = Arrays.equals(target.split("\\s+"), entries[0].translation);

            assertFalse(sameSource && sameTarget);
        }
    }
}
