package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextScore;
import eu.modernmt.context.lucene.analysis.CorpusAnalyzer;
import eu.modernmt.model.Domain;
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
import org.apache.lucene.util.Version;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Created by davide on 10/07/15.
 */
public class ContextAnalyzerIndex implements Closeable, AutoCloseable {

    private static final int MIN_RESULT_BATCH = 20;

    private final Logger logger = LogManager.getLogger(ContextAnalyzerIndex.class);

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
            String name = DocumentBuilder.getName(document);

            logger.info("Adding to index document " + name);

            try {
                this.indexWriter.addDocument(document);
            } catch (IOException e) {
                throw new ContextAnalyzerException("Failed to add document " + name + " to index", e);
            }
        }

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
            throw new ContextAnalyzerException("Unable to clear context analyzer index", e);
        }
    }

    public List<ContextScore> getSimilarDocuments(Corpus queryDocument, int limit) throws ContextAnalyzerException {
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

        // Compute cosine similarity
        List<ContextScore> result = new ArrayList<>(topDocs.length);

        ConsineSimilarityCalculator calculator = new ConsineSimilarityCalculator(reader, DocumentBuilder.CONTENT_FIELD);
        calculator.setAnalyzer(analyzer);
        calculator.setBoost(true);
        calculator.setReferenceDocument(DocumentBuilder.createDocument(queryDocument));
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
            String name = DocumentBuilder.getName(topDoc);

            float similarityScore;
            try {
                similarityScore = calculator.getSimilarity(topDocRef.doc);
            } catch (IOException e) {
                throw new ContextAnalyzerException("Could not compute cosine similarity for doc " + name, e);
            }

            result.add(new ContextScore(new Domain(id, name), similarityScore));
        }

        // Sort and limit result
        Collections.sort(result);
        Collections.reverse(result);

        if (result.size() > limit)
            result = new ArrayList<>(result.subList(0, limit));

        return result;
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(this.indexReader);
        IOUtils.closeQuietly(this.indexWriter);
        IOUtils.closeQuietly(this.indexDirectory);
    }

}
