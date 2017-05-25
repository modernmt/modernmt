package eu.modernmt.decoder.opennmt.memory.lucene;

import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.decoder.opennmt.memory.TranslationMemory;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.BilingualCorpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class LuceneTranslationMemory implements TranslationMemory {

    private final Logger logger = LogManager.getLogger(LuceneTranslationMemory.class);

    private final Directory indexDirectory;
    private final SentenceQueryBuilder queries;
    private final Rescorer rescorer;
    private final IndexWriter indexWriter;

    private DirectoryReader indexReader;
    private Map<Short, Long> channels;

    public LuceneTranslationMemory(File indexPath) throws IOException {
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
    }

    public static Term newIntTerm(String field, int value) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.intToPrefixCoded(value, 0, builder);

        return new Term(field, builder.toBytesRef());
    }

    private synchronized IndexReader getIndexReader() throws IOException {
        if (this.indexReader == null) {
            this.indexReader = DirectoryReader.open(this.indexDirectory);
            this.indexReader.incRef();
        } else {
            DirectoryReader reader = DirectoryReader.openIfChanged(this.indexReader);

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
    public void add(Map<Domain, BilingualCorpus> batch) throws IOException {
        boolean success = false;

        try {
            for (Map.Entry<Domain, BilingualCorpus> entry : batch.entrySet())
                add(entry.getKey().getId(), entry.getValue());

            this.indexWriter.commit();

            success = true;
        } finally {
            if (!success)
                this.indexWriter.rollback();
        }
    }

    @Override
    public void add(Domain domain, BilingualCorpus corpus) throws IOException {
        boolean success = false;

        try {
            add(domain.getId(), corpus);

            this.indexWriter.commit();

            success = true;
        } finally {
            if (!success)
                this.indexWriter.rollback();
        }
    }

    private void add(int domain, BilingualCorpus corpus) throws IOException {
        BilingualCorpus.BilingualLineReader reader = null;

        try {
            reader = corpus.getContentReader();

            long begin = System.currentTimeMillis();

            BilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                Document document = DocumentBuilder.build(domain, pair.source, pair.target);
                this.indexWriter.addDocument(document);
            }

            double elapsed = System.currentTimeMillis() - begin;
            elapsed = (int) (elapsed / 100);
            elapsed /= 10.;

            logger.info("Domain " + domain + " imported in " + elapsed + "s");
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    @Override
    public void add(Domain domain, Sentence sentence, Sentence translation) throws IOException {
        Document document = DocumentBuilder.build(domain.getId(), sentence, translation);
        this.indexWriter.addDocument(document);
        this.indexWriter.commit();
    }

    @Override
    public ScoreEntry[] search(Sentence source, int limit) throws IOException {
        return search(source, null, limit);
    }

    @Override
    public ScoreEntry[] search(Sentence source, ContextVector contextVector, int limit) throws IOException {
        Query query = this.queries.build(source);

        IndexReader reader = this.getIndexReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new CustomSimilarity());

        int queryLimit = Math.max(10, limit * 2);
        ScoreDoc[] docs = searcher.search(query, queryLimit).scoreDocs;

        ScoreEntry[] entries = new ScoreEntry[docs.length];
        for (int i = 0; i < docs.length; i++) {
            entries[i] = DocumentBuilder.parseEntry(searcher.doc(docs[i].doc));
            entries[i].score = docs[i].score;
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
    public void onDataReceived(List<TranslationUnit> batch) throws IOException {
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
