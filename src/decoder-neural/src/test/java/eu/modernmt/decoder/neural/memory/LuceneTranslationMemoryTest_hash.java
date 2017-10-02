package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.neural.memory.lucene.DocumentBuilder;
import eu.modernmt.decoder.neural.memory.lucene.QueryBuilder;
import eu.modernmt.lang.LanguagePair;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static eu.modernmt.decoder.neural.memory.TestData.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by davide on 03/08/17.
 */
public class LuceneTranslationMemoryTest_hash {

    private TLuceneTranslationMemory memory;

    public void setup(LanguagePair... languages) throws Throwable {
        this.memory = new TLuceneTranslationMemory(languages);
    }

    @After
    public void teardown() throws Throwable {
        this.memory.close();
        this.memory = null;
    }

    @Test
    public void queryWithMisleadingHashes() throws Throwable {
        setup(EN__IT, EN__FR);

        IndexWriter indexWriter = memory.getIndexWriter();
        indexWriter.addDocument(DocumentBuilder.build(EN__IT, 2, "2-1", "2-1", "A B C"));
        indexWriter.addDocument(DocumentBuilder.build(EN__IT, 1, "1-1", "1-1", "A B C"));
        indexWriter.addDocument(DocumentBuilder.build(EN__FR, 1, "1-1F", "1-1F", "A B C"));
        indexWriter.addDocument(DocumentBuilder.build(EN__IT, 1, "1-2", "1-2", "C B A"));
        indexWriter.addDocument(DocumentBuilder.build(EN__IT, 1, "1-3", "1-3", "C B Z"));
        indexWriter.commit();

        Query query = QueryBuilder.getByHash(1, EN__IT, "A B C");

        IndexSearcher searcher = memory.getIndexSearcher();
        ScoreDoc[] result = searcher.search(query, 10).scoreDocs;

        assertEquals(1, result.length);

        ScoreEntry entry = DocumentBuilder.parseEntry(EN__IT, searcher.doc(result[0].doc));

        assertArrayEquals(new String[]{"1-1"}, entry.sentence);
    }

    @Test
    public void overwriteNotExisting() throws Throwable {
        setup(EN__IT, EN__FR);

        TranslationUnit original = tu(0, 0L, 1L, EN__IT, "hello world", "ciao mondo", null);
        memory.onDataReceived(Collections.singletonList(original));

        TranslationUnit overwrite = tu(0, 1L, 1L, EN__IT, "test sentence", "frase di prova",
                "hello world __", "ciao mondo __", null);
        memory.onDataReceived(Collections.singletonList(overwrite));

        Set<TLuceneTranslationMemory.Entry> expectedEntries =
                TLuceneTranslationMemory.Entry.asEntrySet(memory.getLanguageIndex(), Arrays.asList(original, overwrite));

        assertEquals(expectedEntries, memory.entrySet());
    }

    @Test
    public void overwriteExisting() throws Throwable {
        setup(EN__IT, EN__FR);

        TranslationUnit original = tu(0, 0L, 1L, EN__IT, "hello world", "ciao mondo", null);
        memory.onDataReceived(Collections.singletonList(original));

        TranslationUnit overwrite = tu(0, 1L, 1L, EN__IT, "test sentence", "frase di prova",
                "hello world", "ciao mondo", null);
        memory.onDataReceived(Collections.singletonList(overwrite));

        Set<TLuceneTranslationMemory.Entry> expectedEntries =
                TLuceneTranslationMemory.Entry.asEntrySet(memory.getLanguageIndex(), Collections.singletonList(overwrite));

        assertEquals(expectedEntries, memory.entrySet());
    }
}
