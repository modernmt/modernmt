package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.lucene.analysis.ContextAnalyzerIndex;
import eu.modernmt.context.lucene.analysis.DocumentBuilder;
import eu.modernmt.context.lucene.analysis.LuceneUtils;
import eu.modernmt.context.lucene.storage.CorporaStorage;
import eu.modernmt.context.lucene.storage.CorpusBucket;
import eu.modernmt.context.lucene.storage.Options;
import eu.modernmt.data.DataBatch;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Memory;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

/**
 * Created by davide on 06/08/17.
 */
public class TLuceneAnalyzer extends LuceneAnalyzer {

    private final File path;

    private static File getTempDirectory() throws IOException {
        return Files.createTempDirectory("TLuceneAnalyzer").toFile();
    }

    public TLuceneAnalyzer() throws IOException {
        this(new Options());
    }

    public TLuceneAnalyzer(Options options) throws IOException {
        this(getTempDirectory(), options);
    }

    private TLuceneAnalyzer(File path, Options options) throws IOException {
        super(path, options);
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

        String docId = DocumentBuilder.makeId(memory, direction);

        // Bucket for content

        String content = null;

        CorpusBucket bucket = storage.getBucket(docId);
        if (bucket != null) {
            InputStream stream = null;

            try {
                stream = bucket.getContentStream();
                content = IOUtils.toString(stream, UTF8Charset.get());
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }

        // Index for terms

        Set<String> terms = null;

        IndexSearcher searcher = index.getIndexSearcher();
        TermQuery query = new TermQuery(DocumentBuilder.makeIdTerm(docId));
        TopDocs docs = searcher.search(query, 1);

        if (docs.scoreDocs.length > 0) {
            String filedName = DocumentBuilder.makeContentFieldName(direction);
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
            this.content = content == null ? null : content.trim();
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

    // DataListener utils

    public void onDelete(final Deletion deletion) throws ContextAnalyzerException {
        super.onDataReceived(new DataBatch() {

            @Override
            public Collection<TranslationUnit> getTranslationUnits() {
                return Collections.emptyList();
            }

            @Override
            public Collection<Deletion> getDeletions() {
                return Collections.singleton(deletion);
            }

            @Override
            public Map<Short, Long> getChannelPositions() {
                return Collections.singletonMap(deletion.channel, deletion.channelPosition);
            }

        });
    }

    public void onDataReceived(Collection<TranslationUnit> units) throws ContextAnalyzerException {
        final HashMap<Short, Long> positions = new HashMap<>();
        for (TranslationUnit unit : units) {
            Long existingPosition = positions.get(unit.channel);

            if (existingPosition == null || existingPosition < unit.channelPosition)
                positions.put(unit.channel, unit.channelPosition);
        }

        super.onDataReceived(new DataBatch() {
            @Override
            public Collection<TranslationUnit> getTranslationUnits() {
                return units;
            }

            @Override
            public Collection<Deletion> getDeletions() {
                return Collections.emptyList();
            }

            @Override
            public Map<Short, Long> getChannelPositions() {
                return positions;
            }
        });
    }

    public void onDataReceived(Memory memory, MultilingualCorpus corpus) throws ContextAnalyzerException {
        Long position = getLatestChannelPositions().getOrDefault((short) 0, 0L);
        if (position == null)
            position = 0L;
        else
            position++;

        ArrayList<TranslationUnit> units = new ArrayList<>();
        MultilingualCorpus.MultilingualLineReader reader = null;
        try {
            reader = corpus.getContentReader();

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                TranslationUnit unit = new TranslationUnit((short) 0, position++, memory.getOwner(), pair.language, memory.getId(),
                        pair.source, pair.target, null, null, new Date(), null, null, null);
                units.add(unit);
            }
        } catch (IOException e) {
            throw new ContextAnalyzerException(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }

        onDataReceived(units);
    }
}
