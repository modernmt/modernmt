package eu.modernmt.context.lucene.analysis.rescoring;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;

import java.io.IOException;

/**
 * Created by davide on 06/08/17.
 */
public interface Rescorer {

    void rescore(IndexReader reader, Analyzer analyzer,
                 ScoreDoc[] topDocs, Document reference, String fieldName) throws IOException;

}
