package eu.modernmt.context.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by davide on 22/09/15.
 */
class IDFTable {

    private ConcurrentHashMap<String, Float> cache;
    private String fieldId;

    public IDFTable(String fieldId) {
        this.fieldId = fieldId;
        this.cache = new ConcurrentHashMap<>();
    }

    public float getTFIDF(IndexReader indexReader, String term, int tf) throws IOException {
        return getTFIDF(indexReader, new Term(fieldId, term), tf);
    }

    public float getTFIDF(IndexReader indexReader, BytesRef term, int tf) throws IOException {
        return getTFIDF(indexReader, new Term(fieldId, term), tf);
    }

    public float getTFIDF(IndexReader indexReader, Term term, int tf) throws IOException {
        String text = term.text();
        float idf;

        try {
            idf = cache.computeIfAbsent(text, s -> {
                try {
                    long df = indexReader.docFreq(term);
                    return idf(indexReader.numDocs(), df);
                } catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
            });
        } catch (RuntimeIOException e) {
            throw e.getIOException();
        }

        return tf(tf) * idf;
    }

    public void invalidate() {
        // TODO: we may implement a smarter strategy to invalidate cache when index changes
        cache.clear();
    }

    private static float tf(int freq) {
        return (float) Math.sqrt(freq);
    }

    private static float idf(long numDocs, long docFreq) {
        return (float) (Math.log(numDocs / (double) (docFreq + 1)) + 1.0);
    }

    private static class RuntimeIOException extends RuntimeException {

        public RuntimeIOException(IOException cause) {
            super(cause);
        }

        public IOException getIOException() {
            return (IOException) getCause();
        }

    }

}
