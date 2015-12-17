package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.context.IndexSourceDocument;
import eu.modernmt.context.lucene.analysis.CorpusAnalyzer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by davide on 10/07/15.
 */
public class ContextAnalyzerIndex implements Closeable, AutoCloseable {

    private static final int MIN_RESULT_BATCH = 20;

    private final Logger logger = LoggerFactory.getLogger(ContextAnalyzerIndex.class);

    private Directory indexDirectory;
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private DirectoryReader indexReader;

    public ContextAnalyzerIndex(File indexPath) throws IOException {
        if (!indexPath.isDirectory())
            FileUtils.forceMkdir(indexPath);

        this.indexDirectory = FSDirectory.open(indexPath);
        this.analyzer = new CorpusAnalyzer();

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

    private synchronized IndexReader getIndexReader() throws IOException {
        if (this.indexReader == null) {
            this.indexReader = DirectoryReader.open(this.indexDirectory);
            this.indexReader.incRef();
        } else {
            DirectoryReader reader = DirectoryReader.openIfChanged(this.indexReader);

            if (reader != null) {
                this.indexReader.close();
                this.indexReader = reader;
                this.indexReader.incRef();
            }
        }

        return this.indexReader;
    }

    public void add(IndexSourceDocument document) throws IOException {
        this.add(Collections.singleton(document));
    }

    public void add(Collection<? extends IndexSourceDocument> documents) throws IOException {
        for (IndexSourceDocument document : documents) {
            logger.info("Adding to index document " + document);
            this.indexWriter.addDocument(DocumentBuilder.createDocument(document));
        }

        this.indexWriter.commit();
    }

    public void clear() throws IOException {
        this.indexWriter.deleteAll();
        this.indexWriter.commit();
    }

    public List<ContextDocument> getSimilarDocuments(IndexSourceDocument queryDocument, int limit) throws IOException {
        IndexReader reader = this.getIndexReader();
        IndexSearcher searcher = new IndexSearcher(reader);

        String fieldName = DocumentBuilder.getContentField(queryDocument);

        // Get matching documents

        int rawLimit = limit < MIN_RESULT_BATCH ? MIN_RESULT_BATCH : limit;

        MoreLikeThis mlt = new MoreLikeThis(reader);
        mlt.setFieldNames(new String[]{fieldName});
        mlt.setMinDocFreq(0);
        mlt.setMinTermFreq(1);
        mlt.setMinWordLen(2);
        mlt.setBoost(true);
        mlt.setAnalyzer(analyzer);

        TopScoreDocCollector collector = TopScoreDocCollector.create(rawLimit, true);
        Query query = mlt.like(fieldName, queryDocument.getContentReader());
        searcher.search(query, collector);

        ScoreDoc[] topDocs = collector.topDocs().scoreDocs;

        // Compute cosine similarity
        List<ContextDocument> result = new ArrayList<>(topDocs.length);

        ConsineSimilarityCalculator calculator = new ConsineSimilarityCalculator(reader, DocumentBuilder.getContentField(queryDocument));
        calculator.setAnalyzer(analyzer);
        calculator.setBoost(true);
        calculator.setReferenceDocument(DocumentBuilder.createDocument(queryDocument));
        calculator.setScoreDocs(topDocs);

        calculator.calculateSimilarity();

        for (int i = 0; i < topDocs.length; i++) {
            ScoreDoc scoreDoc = topDocs[i];
            String name = searcher.doc(scoreDoc.doc).get(DocumentBuilder.DOCUMENT_NAME_FIELD);
            float similarityScore = calculator.getSimilarity(topDocs[i].doc);
            result.add(new ContextDocument(name, similarityScore));
        }

        // Sort and limit result
        Collections.sort(result);
        Collections.reverse(result);

        if (result.size() > limit)
            result = result.subList(0, limit);

        return result;
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(this.indexReader);
        IOUtils.closeQuietly(this.indexWriter);
        IOUtils.closeQuietly(this.indexDirectory);
    }

}
