package eu.modernmt.decoder.neural.memory.lucene.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;

public class DefaultAnalyzerFactory implements AnalyzerFactory {

    private static final int SHINGLE_SIZE = 2;

    @Override
    public Analyzer createContentAnalyzer() {
        return new ContentAnalyzer(SHINGLE_SIZE, true);
    }

    @Override
    public Analyzer createHashAnalyzer() {
        return new HashAnalyzer();
    }

    @Override
    public Analyzer createShortQueryAnalyzer() {
        return new ContentAnalyzer(0, true);
    }

    @Override
    public Analyzer createLongQueryAnalyzer() {
        return new ContentAnalyzer(SHINGLE_SIZE, false);
    }

    @Override
    public Similarity createSimilarity() {
        return new CustomSimilarity();
    }

}
