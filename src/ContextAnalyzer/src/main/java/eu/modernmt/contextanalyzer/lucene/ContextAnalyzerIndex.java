package eu.modernmt.contextanalyzer.lucene;

import eu.modernmt.contextanalyzer.lucene.analysis.CorpusAnalyzer;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
public class ContextAnalyzerIndex implements AutoCloseable {

    private static final int MIN_RESULT_BATCH = 20;

    private final Logger logger = LogManager.getLogger(ContextAnalyzerIndex.class);

    private Directory indexDirectory;
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private DirectoryReader indexReader;

    public ContextAnalyzerIndex(File indexPath) throws IOException {
        if (!indexPath.isDirectory())
            FileUtils.forceMkdir(indexPath);

        this.indexDirectory = FSDirectory.open(indexPath.toPath());
        this.analyzer = new CorpusAnalyzer();

        // Index writer setup
        IndexWriterConfig indexConfig = new IndexWriterConfig(this.analyzer);
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

    public void addCorpus(Corpus corpus) throws IOException {
        this.addCorpora(Collections.singleton(corpus));
    }

    public void addCorpora(Collection<Corpus> corpora) throws IOException {
        for (Corpus corpus : corpora) {
            logger.info("Adding to index corpus " + corpus);
            this.indexWriter.addDocument(DocumentBuilder.createDocument(corpus));
        }
    }

    public void clear() throws IOException {
        this.indexWriter.deleteAll();
        this.indexWriter.commit();
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(this.indexReader);
        IOUtils.closeQuietly(this.indexWriter);
        IOUtils.closeQuietly(this.indexDirectory);
    }

    public List<ScoreDocument> getSimilarDocuments(Corpus corpus, int limit) throws IOException {
        IndexReader reader = this.getIndexReader();
        IndexSearcher searcher = new IndexSearcher(reader);

        String fieldName = DocumentBuilder.getContentField(corpus);

        // Get matching documents

        int rawLimit = limit < MIN_RESULT_BATCH ? MIN_RESULT_BATCH : limit;

        MoreLikeThis mlt = new MoreLikeThis(reader);
        mlt.setFieldNames(new String[]{fieldName});
        mlt.setMinDocFreq(0);
        mlt.setMinTermFreq(1);
        mlt.setMinWordLen(2);
        mlt.setBoost(true);
        mlt.setAnalyzer(analyzer);

        TopScoreDocCollector collector = TopScoreDocCollector.create(rawLimit);
        Query query = mlt.like(fieldName, corpus.getContentReader());
        searcher.search(query, collector);

        ScoreDoc[] topDocs = collector.topDocs().scoreDocs;

        // Create result array

        ScoreDocument[] results = new ScoreDocument[topDocs.length];
        for (int i = 0; i < topDocs.length; i++) {
            ScoreDoc scoreDoc = topDocs[i];
            String name = searcher.doc(scoreDoc.doc).get(DocumentBuilder.DOCUMENT_NAME_FIELD);

            ScoreDocument document = new ScoreDocument(name);
            document.matchingScore = scoreDoc.score;

            results[i] = document;
        }

        // Compute cosine similarity

        ConsineSimilarityCalculator calculator = new ConsineSimilarityCalculator(reader, DocumentBuilder.getContentField(corpus));
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
