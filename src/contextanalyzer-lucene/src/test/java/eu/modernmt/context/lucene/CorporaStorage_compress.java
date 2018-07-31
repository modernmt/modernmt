package eu.modernmt.context.lucene;

import eu.modernmt.context.lucene.analysis.DocumentBuilder;
import eu.modernmt.context.lucene.storage.CorpusBucket;
import eu.modernmt.lang.LanguagePair;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static eu.modernmt.context.lucene.TestData.EN__IT;
import static org.junit.Assert.*;

public class CorporaStorage_compress {

    private TCorporaStorage storage;

    @Before
    public void setup() throws Throwable {
        this.storage = new TCorporaStorage();
    }

    @After
    public void teardown() {
        if (this.storage != null)
            this.storage.close();
        this.storage = null;
    }

    private CorpusBucket getBucket(long memory, LanguagePair language) throws IOException {
        return storage.getBucket(DocumentBuilder.makeId(memory, language));
    }

    private String getContent(CorpusBucket bucket) throws IOException {
        InputStream input = null;

        try {
            input = bucket.getContentStream();
            return IOUtils.toString(input, "UTF-8");
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    @Test
    public void compressExistingStorage() throws Throwable {
        storage.onDataReceived(Arrays.asList(
                TestData.tu(0, 0L, 1L, EN__IT, null),
                TestData.tu(0, 1L, 1L, EN__IT, null),
                TestData.tu(0, 2L, 1L, EN__IT, null),
                TestData.tu(0, 3L, 1L, EN__IT, null),
                TestData.tu(0, 4L, 1L, EN__IT, null),
                TestData.tu(0, 5L, 1L, EN__IT, null),
                TestData.tu(0, 6L, 1L, EN__IT, null),
                TestData.tu(0, 7L, 1L, EN__IT, null)
        ));
        storage.flushToDisk(true, false);

        CorpusBucket bucket = getBucket(1, EN__IT);
        String expectedContent = getContent(bucket);
        long expectedSize = bucket.getSize();

        assertTrue(expectedSize > 0);
        assertTrue(bucket.hasUncompressedContent());

        storage.compress();

        String content = getContent(bucket);
        long size = bucket.getSize();

        assertEquals(expectedSize, size);
        assertFalse(bucket.hasUncompressedContent());
        assertEquals(expectedContent, content);
    }

    @Test
    public void appendAfterCompression() throws Throwable {
        storage.onDataReceived(Arrays.asList(
                TestData.tu(0, 0L, 1L, EN__IT, null),
                TestData.tu(0, 1L, 1L, EN__IT, null),
                TestData.tu(0, 2L, 1L, EN__IT, null),
                TestData.tu(0, 3L, 1L, EN__IT, null)
        ));
        storage.flushToDisk(true, false);

        CorpusBucket bucket = getBucket(1, EN__IT);
        String expectedContent = getContent(bucket);
        long expectedSize = bucket.getSize();

        assertTrue(expectedSize > 0);
        assertTrue(bucket.hasUncompressedContent());

        storage.compress();

        assertFalse(bucket.hasUncompressedContent());

        storage.onDataReceived(Arrays.asList(
                TestData.tu(0, 4L, 1L, EN__IT, null),
                TestData.tu(0, 5L, 1L, EN__IT, null),
                TestData.tu(0, 6L, 1L, EN__IT, null),
                TestData.tu(0, 7L, 1L, EN__IT, null)
        ));
        storage.flushToDisk(true, false);

        assertTrue(bucket.hasUncompressedContent());

        String content = getContent(bucket);
        long size = bucket.getSize();

        assertEquals(expectedSize * 2, size);
        assertEquals(expectedContent + expectedContent, content);
    }

//    @Test
//    public void twoMemories() throws Throwable {
//        List<TranslationUnit> units = Arrays.asList(
//                TestData.tu(0, 0L, 1L, EN__IT, null),
//                TestData.tu(0, 1L, 1L, EN__IT, null),
//                TestData.tu(0, 2L, 1L, EN__IT, null),
//                TestData.tu(0, 3L, 1L, EN__IT, null),
//                TestData.tu(0, 4L, 2L, EN__FR, null),
//                TestData.tu(0, 5L, 2L, EN__FR, null),
//                TestData.tu(0, 6L, 2L, EN__FR, null),
//                TestData.tu(0, 7L, 2L, EN__FR, null)
//        );
//
//        storage.onDataReceived(units);
//
//        String expectedContent1 = getContent(1, EN__IT);
//        String expectedContent2 = getContent(2, EN__FR);
//
//        storage.compress();
//
//        String content1 = getContent(1, EN__IT);
//        String content2 = getContent(2, EN__FR);
//
//        assertEquals(expectedContent1, content1);
//        assertEquals(expectedContent2, content2);
//    }

}
