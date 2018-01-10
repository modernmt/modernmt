package eu.modernmt.decoder.neural.memory.lucene;

import eu.modernmt.data.DataBatch;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.decoder.neural.memory.TranslationMemory;
import eu.modernmt.decoder.neural.memory.lucene.rescoring.F1BleuRescorer;
import eu.modernmt.decoder.neural.memory.lucene.rescoring.Rescorer;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Memory;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

/**
 * Created by davide on 23/05/17.
 */
public class LuceneTranslationMemory implements TranslationMemory {

    private final Logger logger = LogManager.getLogger(LuceneTranslationMemory.class);

    private final int minQuerySize;
    private final Directory indexDirectory;
    private final Rescorer rescorer;
    private final IndexWriter indexWriter;
    private final LanguageIndex languages;
    private DataFilter filter;

    private DirectoryReader _indexReader;
    private IndexSearcher _indexSearcher;
    private final Map<Short, Long> channels;

    private static File forceMkdir(File directory) throws IOException {
        if (!directory.isDirectory())
            FileUtils.forceMkdir(directory);
        return directory;
    }

    public LuceneTranslationMemory(LanguageIndex languages, File indexPath, int minQuerySize) throws IOException {
        this(languages, indexPath, new F1BleuRescorer(), minQuerySize);
    }

    public LuceneTranslationMemory(LanguageIndex languages, Directory directory, int minQuerySize) throws IOException {
        this(languages, directory, new F1BleuRescorer(), minQuerySize);
    }

    public LuceneTranslationMemory(LanguageIndex languages, File indexPath, Rescorer rescorer, int minQuerySize) throws IOException {
        this(languages, FSDirectory.open(forceMkdir(indexPath)), rescorer, minQuerySize);
    }

    public LuceneTranslationMemory(LanguageIndex languages, Directory directory, Rescorer rescorer, int minQuerySize) throws IOException {
        this.indexDirectory = directory;
        this.rescorer = rescorer;
        this.languages = languages;
        this.minQuerySize = minQuerySize;

        // Index writer setup
        IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_4_10_4, Analyzers.getTrainAnalyzer());
        indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        indexConfig.setSimilarity(new CustomSimilarity());

        this.indexWriter = new IndexWriter(this.indexDirectory, indexConfig);

        // Ensure index exists
        if (!DirectoryReader.indexExists(directory))
            this.indexWriter.commit();

        // Read channels status
        IndexSearcher searcher = this.getIndexSearcher();

        Query query = new TermQuery(QueryBuilder.channelsTerm());
        TopDocs docs = searcher.search(query, 1);

        if (docs.scoreDocs.length > 0) {
            Document channelsDocument = searcher.doc(docs.scoreDocs[0].doc);
            this.channels = DocumentBuilder.parseChannels(channelsDocument);
        } else {
            this.channels = new HashMap<>();
        }
    }

    public LanguageIndex getLanguageIndex() {
        return languages;
    }

    public synchronized IndexReader getIndexReader() throws IOException {
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

    // TranslationMemory

    @Override
    /* This method does not store segments hash. Update of content inserted with this method is not possible */
    public void bulkInsert(Map<Memory, MultilingualCorpus> batch) throws IOException {
        boolean success = false;

        try {
            for (Map.Entry<Memory, MultilingualCorpus> entry : batch.entrySet())
                bulkInsert(entry.getKey().getId(), entry.getValue());

            this.indexWriter.commit();

            success = true;
        } finally {
            if (!success)
                this.indexWriter.rollback();
        }
    }

    @Override
    /* This method does not store segments hash. Update of content inserted with this method is not possible */
    public void bulkInsert(Memory memory, MultilingualCorpus corpus) throws IOException {
        boolean success = false;

        try {
            bulkInsert(memory.getId(), corpus);

            this.indexWriter.commit();

            success = true;
        } finally {
            if (!success)
                this.indexWriter.rollback();
        }
    }

    private void bulkInsert(long memory, MultilingualCorpus corpus) throws IOException {
        MultilingualCorpus.MultilingualLineReader reader = null;

        try {
            reader = corpus.getContentReader();

            long begin = System.currentTimeMillis();

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                LanguagePair direction = languages.map(pair.language);

                if (direction != null) {
                    Document document = DocumentBuilder.build(direction, memory, pair.source, pair.target);
                    this.indexWriter.addDocument(document);
                }
            }

            double elapsed = System.currentTimeMillis() - begin;
            elapsed = (int) (elapsed / 100);
            elapsed /= 10.;

            logger.info("Memory " + memory + " imported in " + elapsed + "s");
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    @Override
    public ScoreEntry[] search(LanguagePair direction, Sentence source, int limit) throws IOException {
        return search(direction, source, null, this.rescorer, limit);
    }

    public ScoreEntry[] search(LanguagePair direction, Sentence source, Rescorer rescorer, int limit) throws IOException {
        return this.search(direction, source, null, rescorer, limit);
    }

    @Override
    public ScoreEntry[] search(LanguagePair direction, Sentence source, ContextVector contextVector, int limit) throws IOException {
        return this.search(direction, source, contextVector, this.rescorer, limit);
    }

    public ScoreEntry[] search(LanguagePair direction, Sentence source, ContextVector contextVector, Rescorer rescorer, int limit) throws IOException {
        Query query = QueryBuilder.bestMatchingSuggestion(direction, source);

        IndexSearcher searcher = getIndexSearcher();

        searcher.setSimilarity(new CustomSimilarity());

        int queryLimit = Math.max(this.minQuerySize, limit * 2);
        ScoreDoc[] docs = searcher.search(query, queryLimit).scoreDocs;

        ScoreEntry[] entries = new ScoreEntry[docs.length];
        for (int i = 0; i < docs.length; i++) {
            entries[i] = DocumentBuilder.parseEntry(direction, searcher.doc(docs[i].doc));
            entries[i].score = docs[i].score;
        }

        if (rescorer != null)
            rescorer.rescore(source, entries, contextVector);

        if (entries.length > limit) {
            ScoreEntry[] temp = new ScoreEntry[limit];
            System.arraycopy(entries, 0, temp, 0, limit);
            entries = temp;
        }

        return entries;
    }

    @Override
    public void setDataFilter(DataFilter filter) {
        this.filter = filter;
    }

    // DataListener

    @Override
    public void onDataReceived(DataBatch batch) throws IOException {
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

            Document channelsDocument = DocumentBuilder.build(newChannels);
            this.indexWriter.updateDocument(QueryBuilder.channelsTerm(), channelsDocument);
            this.indexWriter.commit();

            this.channels.putAll(newChannels);

            success = true;
        } finally {
            if (!success)
                this.indexWriter.rollback();
        }
    }

    private void onTranslationUnitsReceived(Collection<TranslationUnit> units) throws IOException {
        DataFilter filter = this.filter;

        for (TranslationUnit unit : units) {
            if (filter != null && !filter.accept(unit))
                continue;

            Long currentPosition = this.channels.get(unit.channel);

            if (currentPosition == null || currentPosition < unit.channelPosition) {
                if (unit.rawPreviousSentence != null && unit.rawPreviousTranslation != null) {
                    String hash = HashGenerator.hash(unit.direction, unit.rawPreviousSentence, unit.rawPreviousTranslation);
                    Query hashQuery = QueryBuilder.getByHash(unit.memory, unit.direction, hash);

                    this.indexWriter.deleteDocuments(hashQuery);
                }

                Document document = DocumentBuilder.build(unit);
                this.indexWriter.addDocument(document);
            }
        }
    }

    private void onDeletionsReceived(Collection<Deletion> deletions) throws IOException {
        for (Deletion deletion : deletions) {
            Long currentPosition = this.channels.get(deletion.channel);

            if (currentPosition == null || currentPosition < deletion.channelPosition)
                this.indexWriter.deleteDocuments(QueryBuilder.memoryTerm(deletion.memory));
        }
    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        return channels;
    }

    // Closeable

    @Override
    public void close() {
        IOUtils.closeQuietly(this._indexReader);
        IOUtils.closeQuietly(this.indexWriter);
        IOUtils.closeQuietly(this.indexDirectory);
    }

}
