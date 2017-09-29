package eu.modernmt.context.lucene;

import eu.modernmt.context.lucene.analysis.ContextAnalyzerIndex;
import eu.modernmt.context.lucene.analysis.DocumentBuilder;
import eu.modernmt.context.lucene.analysis.LuceneUtils;
import eu.modernmt.context.lucene.storage.CorporaStorage;
import eu.modernmt.context.lucene.storage.CorpusBucket;
import eu.modernmt.context.lucene.storage.Options;
import eu.modernmt.io.DefaultCharset;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by davide on 06/08/17.
 */
public class TLuceneAnalyzer extends LuceneAnalyzer {

    private final File path;

    private static File getTempDirectory() throws IOException {
        return Files.createTempDirectory("TLuceneAnalyzer").toFile();
    }

    public TLuceneAnalyzer(LanguagePair... languages) throws IOException {
        this(new Options(), languages);
    }

    public TLuceneAnalyzer(Options options, LanguagePair... languages) throws IOException {
        this(getTempDirectory(), options, languages);
    }

    private TLuceneAnalyzer(File path, Options options, LanguagePair... languages) throws IOException {
        super(new LanguageIndex(Arrays.asList(languages)), path, options);
        this.path = path;
    }

    public int getIndexSize() throws IOException {
        return getIndex().getIndexReader().numDocs();
    }

    public int getStorageSize() {
        return getStorage().size();
    }

    public void flush() throws IOException {
        getStorage().flushToDisk(false, true);
    }

    public Entry getEntry(long memory, LanguagePair direction) throws IOException {
        ContextAnalyzerIndex index = getIndex();
        CorporaStorage storage = getStorage();

        this.flush();

        // Bucket for content

        String content = null;

        CorpusBucket bucket = storage.getBucket(memory, direction);
        if (bucket != null) {
            InputStream stream = null;

            try {
                stream = bucket.getContentStream();
                content = IOUtils.toString(stream, DefaultCharset.get());
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }

        // Index for terms

        Set<String> terms = null;

        IndexSearcher searcher = index.getIndexSearcher();
        TermQuery query = new TermQuery(DocumentBuilder.makeDocumentIdTerm(memory, direction));
        TopDocs docs = searcher.search(query, 1);

        if (docs.scoreDocs.length > 0) {
            String filedName = DocumentBuilder.getContentFieldName(direction);
            terms = LuceneUtils.getTermFrequencies(searcher.getIndexReader(), docs.scoreDocs[0].doc, filedName).keySet();
        }

        // Creating result

        if (terms == null && content == null)
            return null;

        return new Entry(memory, direction, terms, content);
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            FileUtils.deleteDirectory(this.path);
        }
    }

    public static final class Entry {

        public final long memory;
        public final LanguagePair language;
        public final Set<String> terms;
        public final String content;

        public Entry(long memory, LanguagePair language, Set<String> terms, String content) {
            this.memory = memory;
            this.language = language;
            this.terms = Collections.unmodifiableSet(new HashSet<>(terms));
            this.content = content.trim();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (memory != entry.memory) return false;
            return language.equals(entry.language);
        }

        @Override
        public int hashCode() {
            int result = (int) (memory ^ (memory >>> 32));
            result = 31 * result + language.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "memory=" + memory +
                    ", language=" + language +
                    '}';
        }
    }
}
