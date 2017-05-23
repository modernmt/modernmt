package eu.modernmt.decoder.opennmt.storage.lucene;

import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.Sentence;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;

import java.io.IOException;

/**
 * Created by davide on 23/05/17.
 */
class QueryUtils {

    public static Term intTerm(String field, int value) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.intToPrefixCoded(value, 0, builder);

        return new Term(field, builder.toBytesRef());
    }

    public static Document getDocumentByField(IndexReader reader, String field, int value) throws IOException {
        Term term = intTerm(field, value);
        IndexSearcher searcher = new IndexSearcher(reader);

        Query query = new TermQuery(term);
        TopDocs docs = searcher.search(query, 1);

        if (docs.scoreDocs.length < 1)
            return null;

        return searcher.doc(docs.scoreDocs[0].doc);
    }

    public static Query getQuery(Analyzer analyzer, Sentence sentence) {
        String text = TokensOutputStream.toString(sentence, false, true);

        try {
            return new QueryParser(DocumentBuilder.SENTENCE_FIELD, analyzer).parse(text);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
