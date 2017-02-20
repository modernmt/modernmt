package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.lucene.analysis.CorpusAnalyzer;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by davide on 10/07/15.
 */
public class ContextAnalyzerIndex implements Closeable {

    private static final int MIN_RESULT_BATCH = 20;

    private final Logger logger = LogManager.getLogger(ContextAnalyzerIndex.class);

    private IDFTable idfCache;
    private Directory indexDirectory;
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private DirectoryReader indexReader;

    public ContextAnalyzerIndex(File indexPath, Locale language) throws IOException {
        if (!indexPath.isDirectory())
            FileUtils.forceMkdir(indexPath);

        this.indexDirectory = FSDirectory.open(indexPath);
        this.analyzer = new CorpusAnalyzer(language);

        // Index writer setup
        IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_4_10_4, this.analyzer);
        indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        indexConfig.setSimilarity(new DefaultSimilarity() {

            @Override
            public float lengthNorm(FieldInvertState state) {
                return 1.f;
            }

        });

        this.indexWriter = new IndexWriter(this.indexDirectory, indexConfig);
    }

    private IDFTable getIDFCache() throws ContextAnalyzerException {
        if (idfCache == null) {
            synchronized (this) {
                if (idfCache == null) {
                    idfCache = new IDFTable(DocumentBuilder.CONTENT_FIELD);
                }
            }
        }

        return idfCache;
    }

    public void invalidateCache() {
        if (idfCache != null) {
            idfCache.invalidate();

            if (logger.isDebugEnabled())
                logger.debug("IDF cache invalidated");
        }
    }

    private synchronized IndexReader getIndexReader() throws ContextAnalyzerException {
        if (this.indexReader == null) {
            try {
                this.indexReader = DirectoryReader.open(this.indexDirectory);
            } catch (IOException e) {
                throw new ContextAnalyzerException("Could not open index directory: " + this.indexDirectory, e);
            }

            this.indexReader.incRef();
        } else {
            DirectoryReader reader;

            try {
                reader = DirectoryReader.openIfChanged(this.indexReader);
            } catch (IOException e) {
                throw new ContextAnalyzerException("Could not open index directory: " + this.indexDirectory, e);
            }

            if (reader != null) {
                try {
                    this.indexReader.close();
                } catch (IOException e) {
                    logger.warn("Could not close old indexReader", e);
                }

                this.indexReader = reader;
                this.indexReader.incRef();
            }
        }

        return this.indexReader;
    }

    public void add(Document document) throws ContextAnalyzerException {
        this.add(Collections.singleton(document));
    }

    public void add(Collection<Document> documents) throws ContextAnalyzerException {
        for (Document document : documents) {
            int id = DocumentBuilder.getId(document);

            logger.info("Adding to index document " + id);

            try {
                this.indexWriter.addDocument(document);
            } catch (IOException e) {
                throw new ContextAnalyzerException("Failed to add document " + id + " to index", e);
            }
        }
    }

    public void update(int domain, Document document) throws ContextAnalyzerException {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.intToPrefixCoded(domain, 0, builder);

        Term id = new Term(DocumentBuilder.ID_FIELD, builder.toBytesRef());

        try {
            this.indexWriter.updateDocument(id, document);
        } catch (IOException e) {
            throw new ContextAnalyzerException("Unable to update corpus " + domain);
        }
    }

    public void flush() throws ContextAnalyzerException {
        try {
            this.indexWriter.commit();
        } catch (IOException e) {
            throw new ContextAnalyzerException("Unable to commit changes to context analyzer index", e);
        }
    }

    public void clear() throws ContextAnalyzerException {
        try {
            this.indexWriter.deleteAll();
            this.indexWriter.commit();
        } catch (IOException e) {
            throw new ContextAnalyzerException("Unable to drop context analyzer index", e);
        }
    }

    public ContextVector getSimilarDocuments(Corpus queryDocument, int limit) throws ContextAnalyzerException {
        IndexReader reader = this.getIndexReader();
        IndexSearcher searcher = new IndexSearcher(reader);

        // Get matching documents

        int rawLimit = limit < MIN_RESULT_BATCH ? MIN_RESULT_BATCH : limit;

        MoreLikeThis mlt = new MoreLikeThis(reader);
        mlt.setFieldNames(new String[]{DocumentBuilder.CONTENT_FIELD});
        mlt.setMinDocFreq(0);
        mlt.setMinTermFreq(1);
        mlt.setMinWordLen(2);
        mlt.setBoost(true);
        mlt.setAnalyzer(analyzer);

        TopScoreDocCollector collector = TopScoreDocCollector.create(rawLimit, true);

        Reader queryDocumentReader;
        try {
            queryDocumentReader = queryDocument.getRawContentReader();
        } catch (IOException e) {
            throw new ContextAnalyzerException("Could not read content for similar documents query", e);
        }

        try {
            Query query = mlt.like(DocumentBuilder.CONTENT_FIELD, queryDocumentReader);
            searcher.search(query, collector);
        } catch (IOException e) {
            throw new ContextAnalyzerException("Failed to execute MoreLikeThis query", e);
        } finally {
            IOUtils.closeQuietly(queryDocumentReader);
        }

        ScoreDoc[] topDocs = collector.topDocs().scoreDocs;

        if (logger.isTraceEnabled()) {
            for (ScoreDoc doc : topDocs)
                logger.trace("Lucene document score " + doc.doc + ": " + doc.score);
        }

        // Compute cosine similarity
        Document referenceDocument;

        try {
            referenceDocument = DocumentBuilder.createDocument(queryDocument);
        } catch (IOException e) {
            throw new ContextAnalyzerException("Unable to read query document", e);
        }

        ContextVector.Builder resultBuilder = new ContextVector.Builder(topDocs.length);
        resultBuilder.setLimit(limit);

        CosineSimilarityCalculator calculator = new CosineSimilarityCalculator(reader, DocumentBuilder.CONTENT_FIELD, getIDFCache());
        calculator.setAnalyzer(analyzer);
        calculator.setBoost(true);
        calculator.setReferenceDocument(referenceDocument);
        calculator.setScoreDocs(topDocs);

        calculator.calculateSimilarity();

        for (ScoreDoc topDocRef : topDocs) {
            Document topDoc;
            try {
                topDoc = searcher.doc(topDocRef.doc);
            } catch (IOException e) {
                throw new ContextAnalyzerException("Could not resolve document " + topDocRef.doc + " in index", e);
            }

            int id = DocumentBuilder.getId(topDoc);

            float similarityScore;
            try {
                similarityScore = calculator.getSimilarity(topDocRef.doc);
            } catch (IOException e) {
                throw new ContextAnalyzerException("Could not compute cosine similarity for doc " + id, e);
            }

            resultBuilder.add(id, similarityScore);
        }

        return resultBuilder.build();
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(this.indexReader);
        IOUtils.closeQuietly(this.indexWriter);
        IOUtils.closeQuietly(this.indexDirectory);
    }

}
