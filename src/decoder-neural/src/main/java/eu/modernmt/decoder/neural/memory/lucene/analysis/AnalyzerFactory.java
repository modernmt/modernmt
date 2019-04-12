package eu.modernmt.decoder.neural.memory.lucene.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;

public interface AnalyzerFactory {

    Analyzer createContentAnalyzer();

    Analyzer createHashAnalyzer();

    boolean isLongQuery(int queryLength);

    Analyzer createShortQueryAnalyzer();

    Analyzer createLongQueryAnalyzer();

    Similarity createSimilarity();

}
