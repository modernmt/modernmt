package eu.modernmt.contextanalyzer.lucene;

import eu.modernmt.contextanalyzer.lucene.analysis.CorpusAnalyzer;
import eu.modernmt.corpus.Corpus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by davide on 10/07/15.
 */
public class LuceneController {

    private static final Logger logger = LogManager.getLogger(LuceneController.class);

    public enum Status {
        INDEXING, READY, MISSING_INDEX
    }

    private File indexPath;
    private Analyzer analyzer;
    private IndexSearcher index;
    private Status status;

    public LuceneController(File indexPath) {
        this.indexPath = indexPath;
        this.analyzer = new CorpusAnalyzer();
        this.index = createIndexSearcher();
        this.status = this.index == null ? Status.MISSING_INDEX : Status.READY;
    }

    private IndexSearcher createIndexSearcher() {
        try {
            Directory directory = FSDirectory.open(indexPath.toPath());
            IndexReader indexReader = DirectoryReader.open(directory);
            return new IndexSearcher(indexReader);
        } catch (IOException e) {
            logger.warn("Unable to load index at " + indexPath, e);
            return null;
        }
    }

    public synchronized Status getStatus() {
        return this.status;
    }

    private synchronized void setStatus(Status status) {
        this.status = status;

        if (status == Status.READY)
            this.index = createIndexSearcher();
        else
            this.index = null;
    }

    public void reindex(Collection<Corpus> corpora) throws IOException {
        this.setStatus(Status.INDEXING);

        Directory directory = FSDirectory.open(indexPath.toPath());

        IndexWriterConfig indexConfig = new IndexWriterConfig(this.analyzer);
        indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        indexConfig.setSimilarity(new DefaultSimilarity() {

            @Override
            public float lengthNorm(FieldInvertState state) {
                return 1.f;
            }

        });
        IndexWriter writer = new IndexWriter(directory, indexConfig);

        try {
            for (Corpus corpus : corpora) {
                logger.info("Indexing " + corpus);
                writer.addDocument(DocumentBuilder.createDocument(corpus));
            }

            writer.forceMerge(1);
        } finally {
            writer.close();
        }

        this.setStatus(Status.READY);
    }

    public List<ScoreDocument> getSimilarDocuments(Corpus corpus, int limit) throws IOException {
        Status status = this.getStatus();

        if (!Status.READY.equals(status))
            throw new IllegalStateException("Invalid controller state: " + status);

        IndexReader reader = this.index.getIndexReader();
        String fieldName = DocumentBuilder.getContentField(corpus);

        // Get matching documents

        int rawLimit = limit < 10 ? 10 : limit;

        MoreLikeThis mlt = new MoreLikeThis(reader);
        mlt.setFieldNames(new String[]{fieldName});
        mlt.setMinDocFreq(0);
        mlt.setMinTermFreq(1);
        mlt.setMinWordLen(2);
        mlt.setBoost(true);
        mlt.setAnalyzer(analyzer);

        TopScoreDocCollector collector = TopScoreDocCollector.create(rawLimit);
        Query query = mlt.like(fieldName, corpus.getContentReader());
        this.index.search(query, collector);

        ScoreDoc[] topDocs = collector.topDocs().scoreDocs;

        // Create result array

        ScoreDocument[] results = new ScoreDocument[topDocs.length];
        for (int i = 0; i < topDocs.length; i++) {
            ScoreDoc scoreDoc = topDocs[i];
            String name = this.index.doc(scoreDoc.doc).get(DocumentBuilder.DOCUMENT_NAME_FIELD);

            ScoreDocument document = new ScoreDocument(name);
            document.matchingScore = scoreDoc.score;

            results[i] = document;
        }

        // Compute cosine similarity

        ConsineSimilarityCalculator calculator = new ConsineSimilarityCalculator(index.getIndexReader(), DocumentBuilder.getContentField(corpus));
        calculator.setAnalyzer(analyzer);
        calculator.setBoost(true);
        calculator.setReferenceDocument(DocumentBuilder.createDocument(corpus));
        calculator.setScoreDocs(topDocs);

        calculator.calculateSimilarity();

        for (int i = 0; i < topDocs.length; i++)
            results[i].similarityScore = calculator.getSimilarity(topDocs[i].doc);

        // Sort and limit result

        List<ScoreDocument> list = Arrays.asList(results);
        Collections.sort(list);
        Collections.reverse(list);

        if (list.size() > limit)
            list = list.subList(0, limit);

        return list;
    }

}
