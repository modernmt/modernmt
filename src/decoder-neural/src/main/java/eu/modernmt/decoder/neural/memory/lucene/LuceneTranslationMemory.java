package eu.modernmt.decoder.neural.memory.lucene;

import eu.modernmt.data.DataBatch;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.HashGenerator;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.memory.TranslationMemory;
import eu.modernmt.decoder.neural.memory.lucene.analysis.AnalyzerFactory;
import eu.modernmt.decoder.neural.memory.lucene.analysis.DefaultAnalyzerFactory;
import eu.modernmt.decoder.neural.memory.lucene.query.DefaultQueryBuilder;
import eu.modernmt.decoder.neural.memory.lucene.query.QueryBuilder;
import eu.modernmt.decoder.neural.memory.lucene.query.rescoring.F1BleuRescorer;
import eu.modernmt.decoder.neural.memory.lucene.query.rescoring.Rescorer;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.apache.lucene.analysis.Analyzer.PER_FIELD_REUSE_STRATEGY;

/**
 * Created by davide on 23/05/17.
 */
public class LuceneTranslationMemory implements TranslationMemory {

    protected final Logger logger = LogManager.getLogger(LuceneTranslationMemory.class);

    protected final int minQuerySize;
    protected final Directory indexDirectory;
    protected final QueryBuilder queryBuilder;
    protected final Rescorer rescorer;
    protected final AnalyzerFactory analyzerFactory;
    protected final DocumentBuilder documentBuilder;
    protected final Analyzer shortQueryAnalyzer;
    protected final Analyzer longQueryAnalyzer;
    protected final IndexWriter indexWriter;

    private DirectoryReader _indexReader;
    private IndexSearcher _indexSearcher;
    private final Map<Short, Long> channels;

    private boolean closed = false;

    protected static File forceMkdir(File directory) throws IOException {
        if (!directory.isDirectory())
            FileUtils.forceMkdir(directory);
        return directory;
    }

    public LuceneTranslationMemory(File indexPath, int minQuerySize) throws IOException {
        this(indexPath, new F1BleuRescorer(), minQuerySize);
    }

    public LuceneTranslationMemory(Directory directory, int minQuerySize) throws IOException {
        this(directory, new F1BleuRescorer(), minQuerySize);
    }

    public LuceneTranslationMemory(File indexPath, Rescorer rescorer, int minQuerySize) throws IOException {
        this(FSDirectory.open(forceMkdir(indexPath)), rescorer, minQuerySize);
    }

    public LuceneTranslationMemory(Directory directory, Rescorer rescorer, int minQuerySize) throws IOException {
        this(directory, new DefaultDocumentBuilder(), new DefaultQueryBuilder(), rescorer, new DefaultAnalyzerFactory(), minQuerySize);
    }

    public LuceneTranslationMemory(File indexPath, DocumentBuilder documentBuilder, QueryBuilder queryBuilder, Rescorer rescorer, AnalyzerFactory analyzerFactory, int minQuerySize) throws IOException {
        this(FSDirectory.open(forceMkdir(indexPath)), documentBuilder, queryBuilder, rescorer, analyzerFactory, minQuerySize);
    }

    public LuceneTranslationMemory(Directory directory, DocumentBuilder documentBuilder, QueryBuilder queryBuilder, Rescorer rescorer, AnalyzerFactory analyzerFactory, int minQuerySize) throws IOException {
        this.indexDirectory = directory;
        this.queryBuilder = queryBuilder;
        this.rescorer = rescorer;
        this.documentBuilder = documentBuilder;
        this.analyzerFactory = analyzerFactory;
        this.shortQueryAnalyzer = analyzerFactory.createShortQueryAnalyzer();
        this.longQueryAnalyzer = analyzerFactory.createLongQueryAnalyzer();
        this.minQuerySize = minQuerySize;

        // Index writer setup
        IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_4_10_4, new DelegatingAnalyzerWrapper(PER_FIELD_REUSE_STRATEGY) {
            @Override
            protected Analyzer getWrappedAnalyzer(String fieldName) {
                if (documentBuilder.isHashField(fieldName))
                    return analyzerFactory.createHashAnalyzer();
                else
                    return analyzerFactory.createContentAnalyzer();
            }
        });

        indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        indexConfig.setSimilarity(analyzerFactory.createSimilarity());

        this.indexWriter = new IndexWriter(this.indexDirectory, indexConfig);

        // Ensure index exists
        if (!DirectoryReader.indexExists(directory))
            this.indexWriter.commit();

        // Read channels status
        IndexSearcher searcher = this.getIndexSearcher();

        Query query = this.queryBuilder.getChannels(this.documentBuilder);
        TopDocs docs = searcher.search(query, 1);

        if (docs.scoreDocs.length > 0) {
            Document channelsDocument = searcher.doc(docs.scoreDocs[0].doc);
            this.channels = this.documentBuilder.asChannels(channelsDocument);
        } else {
            this.channels = new HashMap<>();
        }
    }

    protected synchronized IndexReader getIndexReader() throws IOException {
        if (this._indexReader == null) {
            this._indexReader = DirectoryReader.open(this.indexDirectory);
            this._indexReader.incRef();
            this._indexSearcher = new IndexSearcher(this._indexReader);
        } else {
            DirectoryReader reader = DirectoryReader.openIfChanged(this._indexReader);

            if (reader != null) {
                this._indexReader.close();
                this._indexReader = reader;
                this._indexReader.incRef();

                this._indexSearcher = new IndexSearcher(this._indexReader);
                this._indexSearcher.setSimilarity(analyzerFactory.createSimilarity());
            }
        }

        return this._indexReader;
    }

    public IndexSearcher getIndexSearcher() throws IOException {
        getIndexReader();
        return this._indexSearcher;
    }

    public IndexWriter getIndexWriter() {
        return this.indexWriter;
    }

    @Override
    public int size() {
        try {
            IndexReader reader = getIndexReader();
            return Math.max(reader.numDocs(), reader.maxDoc() - 1);
        } catch (IOException e) {
            logger.warn("Error while invoking getIndexReader()", e);
            return 0;
        }
    }

    @Override
    public void dump(long memory, Consumer<Entry> consumer) throws IOException {
        IndexSearcher searcher = getIndexSearcher();
        IndexReader reader = getIndexReader();

        int size = reader.numDocs();
        if (size == 0)
            return;

        Query memoryQuery = new TermQuery(documentBuilder.makeMemoryTerm(memory));
        TopDocs docs = searcher.search(memoryQuery, size);

        for (ScoreDoc scoreDoc : docs.scoreDocs) {
            Document document = reader.document(scoreDoc.doc);
            if (documentBuilder.getMemory(document) > 0) {
                TranslationMemory.Entry entry = documentBuilder.asEntry(document);
                consumer.accept(entry);
            }
        }
    }

    @Override
    public void dumpAll(Consumer<Entry> consumer) throws IOException {
        IndexSearcher searcher = getIndexSearcher();
        IndexReader reader = getIndexReader();

        int size = reader.numDocs();
        if (size == 0)
            return;

        TopDocs docs = searcher.search(new MatchAllDocsQuery(), size);

        for (ScoreDoc scoreDoc : docs.scoreDocs) {
            Document document = reader.document(scoreDoc.doc);
            if (documentBuilder.getMemory(document) > 0) {
                TranslationMemory.Entry entry = documentBuilder.asEntry(document);
                consumer.accept(entry);
            }
        }
    }

    // TranslationMemory

    @Override
    public ScoreEntry[] search(UUID user, LanguageDirection direction, Sentence source, ContextVector contextVector, int limit) throws IOException {
        return search(user, direction, source, contextVector, this.rescorer, limit);
    }

    public ScoreEntry[] search(UUID user, LanguageDirection direction, Sentence source, ContextVector contextVector, Rescorer rescorer, int limit) throws IOException {
        Analyzer analyzer = this.queryBuilder.isLongQuery(source.getWords().length) ? longQueryAnalyzer : shortQueryAnalyzer;
        Query query = this.queryBuilder.bestMatchingSuggestion(documentBuilder, analyzer, user, direction, source, contextVector);

        IndexSearcher searcher = getIndexSearcher();

        int queryLimit = Math.max(this.minQuerySize, limit * 2);
        ScoreDoc[] docs = searcher.search(query, queryLimit).scoreDocs;

        ScoreEntry[] entries = new ScoreEntry[docs.length];
        for (int i = 0; i < docs.length; i++) {
            entries[i] = documentBuilder.asScoreEntry(searcher.doc(docs[i].doc), direction);
            entries[i].score = docs[i].score;
        }

        if (rescorer != null)
            entries = rescorer.rescore(direction, source, entries, contextVector);

        if (entries.length > limit) {
            ScoreEntry[] temp = new ScoreEntry[limit];
            System.arraycopy(entries, 0, temp, 0, limit);
            entries = temp;
        }

        return entries;
    }

    @Override
    public synchronized void optimize() throws IOException {
        IndexReader reader = getIndexReader();
        logger.info("Starting memory forced merge " +
                "(deleted-docs = " + reader.numDeletedDocs() + ", size = " + reader.numDocs() + ", max-doc = " + reader.maxDoc());

        long begin = System.currentTimeMillis();
        this.indexWriter.forceMerge(1);
        this.indexWriter.commit();
        long elapsed = System.currentTimeMillis() - begin;

        reader = getIndexReader();
        logger.info("Memory forced merge completed in " + (elapsed / 1000.) + "s " +
                "(deleted-docs = " + reader.numDeletedDocs() + ", size = " + reader.numDocs() + ", max-doc = " + reader.maxDoc());
    }

    // DataListener

    @Override
    public synchronized void onDataReceived(DataBatch batch) throws IOException {
        if (closed)
            return;

        boolean success = false;

        try {
            this.onTranslationUnitsReceived(batch.getTranslationUnits());
            this.onDeletionsReceived(batch.getDeletions());

            // Writing channels
            HashMap<Short, Long> newChannels = new HashMap<>(this.channels);
            for (Map.Entry<Short, Long> entry : batch.getChannelPositions().entrySet()) {
                Long position = entry.getValue();
                Long existingPosition = newChannels.get(entry.getKey());

                if (existingPosition == null || existingPosition < position)
                    newChannels.put(entry.getKey(), position);
            }

            Document channelsDocument = documentBuilder.create(newChannels);
            this.indexWriter.updateDocument(documentBuilder.makeChannelsTerm(), channelsDocument);
            this.indexWriter.commit();

            this.channels.putAll(newChannels);

            success = true;
        } finally {
            if (!success)
                this.indexWriter.rollback();
        }
    }

    @Override
    public boolean needsProcessing() {
        return true;
    }

    @Override
    public boolean needsAlignment() {
        return false;
    }

    private void onTranslationUnitsReceived(Collection<TranslationUnit> units) throws IOException {
        for (TranslationUnit unit : units) {
            Long currentPosition = this.channels.get(unit.channel);

            if (currentPosition == null || currentPosition < unit.channelPosition) {
                if (unit.rawPreviousSentence != null && unit.rawPreviousTranslation != null) {
                    String hash = HashGenerator.hash(unit.rawLanguage, unit.rawPreviousSentence, unit.rawPreviousTranslation);
                    Query hashQuery = this.queryBuilder.getByHash(documentBuilder, unit.memory, hash);

                    this.indexWriter.deleteDocuments(hashQuery);
                }

                Document document = documentBuilder.create(unit);
                this.indexWriter.addDocument(document);
            }
        }
    }

    private void onDeletionsReceived(Collection<Deletion> deletions) throws IOException {
        for (Deletion deletion : deletions) {
            Long currentPosition = this.channels.get(deletion.channel);

            if (currentPosition == null || currentPosition < deletion.channelPosition)
                this.indexWriter.deleteDocuments(documentBuilder.makeMemoryTerm(deletion.memory));
        }
    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        return channels;
    }

    // Closeable

    @Override
    public synchronized void close() throws IOException {
        this.closed = true;

        IOException error = null;

        try {
            if (this._indexReader != null)
                this._indexReader.close();
        } catch (IOException e) {
            error = e;
        }

        try {
            if (this.indexWriter != null)
                this.indexWriter.close();
        } catch (IOException e) {
            if (error == null)
                error = e;
        }

        try {
            if (this.indexDirectory != null)
                this.indexDirectory.close();
        } catch (IOException e) {
            if (error == null)
                error = e;
        }

        if (error != null)
            throw error;
    }

}
