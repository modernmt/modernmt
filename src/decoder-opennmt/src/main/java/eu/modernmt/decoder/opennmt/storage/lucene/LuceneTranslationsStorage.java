package eu.modernmt.decoder.opennmt.storage.lucene;

import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.opennmt.storage.ScoreEntry;
import eu.modernmt.decoder.opennmt.storage.StorageException;
import eu.modernmt.decoder.opennmt.storage.TranslationsStorage;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.BilingualCorpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public class LuceneTranslationsStorage implements TranslationsStorage {

    private final Directory indexDirectory;
    private final SentenceQueryBuilder queries;
    private final Rescorer rescorer;
    private final IndexWriter indexWriter;

    private DirectoryReader indexReader;
    private Map<Short, Long> channels;

    public LuceneTranslationsStorage(File indexPath) throws StorageException {
        try {
            if (!indexPath.isDirectory())
                FileUtils.forceMkdir(indexPath);

            this.indexDirectory = FSDirectory.open(indexPath);
            this.queries = new SentenceQueryBuilder();
            this.rescorer = new Rescorer();

            // Index writer setup
            IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_4_10_4, Analyzers.getTrainAnalyzer());
            indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            indexConfig.setSimilarity(new CustomSimilarity());

            this.indexWriter = new IndexWriter(this.indexDirectory, indexConfig);

            // Read channels status
            if (DirectoryReader.indexExists(this.indexDirectory)) {
                IndexReader reader = this.getIndexReader();

                Term term = newIntTerm(DocumentBuilder.DOMAIN_ID_FIELD, 0);
                IndexSearcher searcher = new IndexSearcher(reader);

                Query query = new TermQuery(term);
                TopDocs docs = searcher.search(query, 1);

                if (docs.scoreDocs.length > 0) {
                    Document channelsDocument = searcher.doc(docs.scoreDocs[0].doc);
                    this.channels = DocumentBuilder.parseChannels(channelsDocument);
                } else {
                    this.channels = new HashMap<>();
                }
            }
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    public static Term newIntTerm(String field, int value) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.intToPrefixCoded(value, 0, builder);

        return new Term(field, builder.toBytesRef());
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
                Document document = DocumentBuilder.build(domainId, pair.source, pair.target);

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
    public ScoreEntry[] search(Sentence source, int limit) throws StorageException {
        return search(source, null, limit);
    }

    @Override
    public ScoreEntry[] search(Sentence source, ContextVector contextVector, int limit) throws StorageException {
        Query query = this.queries.build(source);

        IndexReader reader = this.getIndexReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new CustomSimilarity());

        ScoreEntry[] entries;

        try {
            int queryLimit = Math.max(10, limit * 2);
            ScoreDoc[] docs = searcher.search(query, queryLimit).scoreDocs;

            entries = new ScoreEntry[docs.length];
            for (int i = 0; i < docs.length; i++) {
                entries[i] = DocumentBuilder.parseEntry(searcher.doc(docs[i].doc));
                entries[i].score = docs[i].score;
            }
        } catch (IOException e) {
            throw new StorageException("Failed to retrieve translations", e);
        }

        rescorer.score(source, entries, contextVector);

        if (entries.length > limit) {
            ScoreEntry[] temp = new ScoreEntry[limit];
            System.arraycopy(entries, 0, temp, 0, limit);
            entries = temp;
        }

        return entries;
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

            Term id = newIntTerm(DocumentBuilder.DOMAIN_ID_FIELD, 0);
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
        Term deleteId = newIntTerm(DocumentBuilder.DOMAIN_ID_FIELD, deletion.domain);
        this.indexWriter.deleteDocuments(deleteId);

        this.channels.put(deletion.channel, deletion.channelPosition);

        Term channelsId = newIntTerm(DocumentBuilder.DOMAIN_ID_FIELD, 0);
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
