package eu.modernmt.decoder.opennmt.storage.lucene;

import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.opennmt.storage.StorageException;
import eu.modernmt.decoder.opennmt.storage.TranslationsStorage;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.BilingualCorpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by davide on 23/05/17.
 */
public class LuceneTranslationsStorage implements TranslationsStorage {

    private static final Pattern WhitespaceRegex = Pattern.compile("\\s+");

    private final Directory indexDirectory;
    private final Analyzer analyzer;
    private final IndexWriter indexWriter;

    private DirectoryReader indexReader;
    private Map<Short, Long> channels;

    public LuceneTranslationsStorage(File indexPath) throws StorageException {
        try {
            if (!indexPath.isDirectory())
                FileUtils.forceMkdir(indexPath);

            this.indexDirectory = FSDirectory.open(indexPath);
            this.analyzer = new WhitespaceAnalyzer();

            // Index writer setup
            IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_4_10_4, this.analyzer);
            indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            this.indexWriter = new IndexWriter(this.indexDirectory, indexConfig);

            // Read channels status
            if (DirectoryReader.indexExists(this.indexDirectory)) {
                IndexReader reader = this.getIndexReader();
                Document channelsDocument = QueryUtils.getDocumentByField(reader, DocumentBuilder.DOMAIN_ID_FIELD, 0);

                this.channels = channelsDocument == null ? new HashMap<>() : DocumentBuilder.parseChannels(channelsDocument);
            }
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    private synchronized IndexReader getIndexReader() throws StorageException {
        if (this.indexReader == null) {
            try {
                this.indexReader = DirectoryReader.open(this.indexDirectory);
            } catch (IOException e) {
                throw new StorageException("Could not open index directory: " + this.indexDirectory, e);
            }

            this.indexReader.incRef();
        } else {
            DirectoryReader reader;

            try {
                reader = DirectoryReader.openIfChanged(this.indexReader);
            } catch (IOException e) {
                throw new StorageException("Could not open index directory: " + this.indexDirectory, e);
            }

            if (reader != null) {
                IOUtils.closeQuietly(this.indexReader);
                this.indexReader = reader;
                this.indexReader.incRef();
            }
        }

        return this.indexReader;
    }

    // TranslationStorage

    @Override
    public void add(Domain domain, BilingualCorpus corpus) throws StorageException, IOException {
        int domainId = domain.getId();

        BilingualCorpus.BilingualLineReader reader = null;
        boolean success = false;

        try {
            reader = corpus.getContentReader();

            BilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                Document document = DocumentBuilder.build(domainId,
                        WhitespaceRegex.split(pair.source), WhitespaceRegex.split(pair.target));

                try {
                    this.indexWriter.addDocument(document);
                } catch (IOException e) {
                    throw new StorageException(e);
                }
            }

            try {
                this.indexWriter.commit();
            } catch (IOException e) {
                throw new StorageException(e);
            }

            success = true;
        } finally {
            IOUtils.closeQuietly(reader);

            if (!success)
                this.indexWriter.rollback();
        }
    }

    @Override
    public void add(Domain domain, Sentence sentence, Sentence translation) throws StorageException {
        Document document = DocumentBuilder.build(domain.getId(), sentence, translation);

        try {
            this.indexWriter.addDocument(document);
            this.indexWriter.commit();
        } catch (IOException e) {
            throw new StorageException(e);
        }

    }

    @Override
    public SearchResult search(Sentence source, int limit) throws StorageException {
        return search(source, null, limit);
    }

    @Override
    public SearchResult search(Sentence source, ContextVector contextVector, int limit) throws StorageException {
        Query query = QueryUtils.getQuery(this.analyzer, source);

        IndexReader reader = this.getIndexReader();
        IndexSearcher searcher = new IndexSearcher(reader);

        Entry[] entries;
        float[] scores;

        try {
            ScoreDoc[] docs = searcher.search(query, limit).scoreDocs;

            entries = new Entry[docs.length];
            scores = new float[docs.length];

            for (int i = 0; i < docs.length; i++) {
                entries[i] = DocumentBuilder.parseEntry(searcher.doc(docs[i].doc));
                scores[i] = docs[i].score;
            }
        } catch (IOException e) {
            throw new StorageException("Failed to retrieve translations", e);
        }

        return new SearchResult(entries, scores);
    }

    // DataListener

    @Override
    public void onDataReceived(List<TranslationUnit> batch) throws Exception {
        boolean success = false;

        HashMap<Short, Long> newChannels = new HashMap<>(this.channels);

        try {
            for (TranslationUnit unit : batch) {
                Long currentPosition = newChannels.get(unit.channel);

                if (currentPosition == null || currentPosition < unit.channelPosition) {
                    newChannels.put(unit.channel, unit.channelPosition);

                    Document document = DocumentBuilder.build(unit);
                    this.indexWriter.addDocument(document);
                }
            }

            Term id = QueryUtils.intTerm(DocumentBuilder.DOMAIN_ID_FIELD, 0);
            Document channelsDocument = DocumentBuilder.build(newChannels);
            this.indexWriter.updateDocument(id, channelsDocument);

            this.indexWriter.commit();

            this.channels.putAll(newChannels);
            success = true;
        } finally {
            if (!success)
                this.indexWriter.rollback();
        }
    }

    @Override
    public void onDelete(Deletion deletion) throws Exception {
        Term deleteId = QueryUtils.intTerm(DocumentBuilder.DOMAIN_ID_FIELD, deletion.domain);
        this.indexWriter.deleteDocuments(deleteId);

        this.channels.put(deletion.channel, deletion.channelPosition);

        Term channelsId = QueryUtils.intTerm(DocumentBuilder.DOMAIN_ID_FIELD, 0);
        Document channelsDocument = DocumentBuilder.build(this.channels);
        this.indexWriter.updateDocument(channelsId, channelsDocument);
        this.indexWriter.commit();
    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        return channels;
    }

    // Closeable

    @Override
    public void close() {
        IOUtils.closeQuietly(this.indexReader);
        IOUtils.closeQuietly(this.indexWriter);
        IOUtils.closeQuietly(this.indexDirectory);
    }

}
