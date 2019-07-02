package eu.modernmt.backup.model;

import eu.modernmt.data.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class BackupMemory implements Closeable, DataListener {

    private final Logger logger = LogManager.getLogger(BackupMemory.class);

    private final Directory indexDirectory;
    private final IndexWriter indexWriter;

    private DirectoryReader _indexReader;
    private IndexSearcher _indexSearcher;
    private final Map<Short, Long> channels;

    private boolean closed = false;

    private static File forceMkdir(File directory) throws IOException {
        if (!directory.isDirectory())
            FileUtils.forceMkdir(directory);
        return directory;
    }

    public BackupMemory(File indexPath) throws IOException {
        this(FSDirectory.open(forceMkdir(indexPath)));
    }

    public BackupMemory(Directory directory) throws IOException {
        this.indexDirectory = directory;

        // Index writer setup
        IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_4_10_4, new WhitespaceAnalyzer());
        indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        this.indexWriter = new IndexWriter(this.indexDirectory, indexConfig);

        // Ensure index exists
        if (!DirectoryReader.indexExists(directory))
            this.indexWriter.commit();

        // Read channels status
        IndexSearcher searcher = this.getIndexSearcher();

        Query query = new TermQuery(DocumentBuilder.makeChannelsTerm());
        TopDocs docs = searcher.search(query, 1);

        if (docs.scoreDocs.length > 0) {
            Document channelsDocument = searcher.doc(docs.scoreDocs[0].doc);
            this.channels = DocumentBuilder.asChannels(channelsDocument);
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

    public void dump(final Consumer<BackupEntry> consumer) throws IOException {
        IndexSearcher searcher = getIndexSearcher();
        IndexReader reader = getIndexReader();

        int size = reader.numDocs();
        if (size == 0)
            return;

        searcher.search(new MatchAllDocsQuery(), null, new Collector() {
            private IndexReader currentReader;

            @Override
            public void setScorer(Scorer scorer) {
                // Ignore scorer
            }

            @Override
            public void collect(int doc) throws IOException {
                Document document = currentReader.document(doc);
                if (DocumentBuilder.getMemory(document) > 0) {
                    BackupEntry entry = DocumentBuilder.asEntry(document);
                    consumer.accept(entry);
                }
            }

            @Override
            public void setNextReader(AtomicReaderContext context) {
                this.currentReader = context.reader();
            }

            @Override
            public boolean acceptsDocsOutOfOrder() {
                return true;
            }
        });
    }

    public synchronized void optimize() throws IOException {
        logger.info("Starting backup memory forced merge");
        long begin = System.currentTimeMillis();
        this.indexWriter.forceMerge(1);
        this.indexWriter.commit();
        long elapsed = System.currentTimeMillis() - begin;
        logger.info("BackupFile memory forced merge completed in " + (elapsed / 1000.) + "s");
    }

    private Query getHashQuery(long memory, String hash) {
        PhraseQuery hashQuery = new PhraseQuery();
        for (String h : hash.split(" "))
            hashQuery.add(DocumentBuilder.makeHashTerm(h));

        TermQuery memoryQuery = new TermQuery(DocumentBuilder.makeMemoryTerm(memory));

        BooleanQuery query = new BooleanQuery();
        query.add(hashQuery, BooleanClause.Occur.MUST);
        query.add(memoryQuery, BooleanClause.Occur.MUST);

        return query;
    }

    // DataListener

    @Override
    public synchronized void onDataReceived(DataBatch batch) throws IOException {
        if (closed)
            return;

        boolean success = false;

        try {
            this.onTranslationUnitsReceived(batch.getDiscardedTranslationUnits());
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

            Document channelsDocument = DocumentBuilder.newChannelsInstance(newChannels);
            this.indexWriter.updateDocument(DocumentBuilder.makeChannelsTerm(), channelsDocument);
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
        return false;
    }

    @Override
    public boolean needsAlignment() {
        return false;
    }

    @Override
    public boolean includeDiscardedTranslationUnits() {
        return true;
    }

    private void onTranslationUnitsReceived(Collection<TranslationUnit> units) throws IOException {
        for (TranslationUnit unit : units) {
            Long currentPosition = this.channels.get(unit.channel);

            if (currentPosition == null || currentPosition < unit.channelPosition) {
                if (unit.rawPreviousSentence != null && unit.rawPreviousTranslation != null) {
                    String hash = HashGenerator.hash(unit.rawLanguage, unit.rawPreviousSentence, unit.rawPreviousTranslation);
                    Query hashQuery = getHashQuery(unit.memory, hash);

                    this.indexWriter.deleteDocuments(hashQuery);
                }

                Document document = DocumentBuilder.newInstance(unit);
                this.indexWriter.addDocument(document);
            }
        }
    }

    private void onDeletionsReceived(Collection<Deletion> deletions) throws IOException {
        for (Deletion deletion : deletions) {
            Long currentPosition = this.channels.get(deletion.channel);

            if (currentPosition == null || currentPosition < deletion.channelPosition)
                this.indexWriter.deleteDocuments(DocumentBuilder.makeMemoryTerm(deletion.memory));
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