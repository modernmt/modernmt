package eu.modernmt.context.lucene.analysis.rescoring;

import eu.modernmt.context.lucene.analysis.LuceneUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.ScoreDoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by davide on 06/08/17.
 */
public class CosineSimilarityRescorer implements Rescorer {

    @Override
    public void rescore(IndexReader reader, Analyzer analyzer, ScoreDoc[] topDocs, Document reference, String fieldName) throws IOException {
        // Compute reference document stats
        HashMap<String, Float> referenceTerms = LuceneUtils.getTermFrequencies(analyzer, reference, fieldName);
        double referenceL2Norm = getL2Norm(referenceTerms);

        // Calculate similarity with reference
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        Future<?>[] tasks = new Future<?>[topDocs.length];
        for (int i = 0; i < tasks.length; i++)
            tasks[i] = executor.submit(new RescoringTask(reader, fieldName, topDocs[i], referenceTerms, referenceL2Norm));

        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (InterruptedException e) {
                throw new IOException("Execution interrupted", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new Error("Expected exception", e);
                }
            }
        }
    }

    public static double getL2Norm(HashMap<String, Float> terms) throws IOException {
        double norm = 0;

        for (Float value : terms.values())
            norm += value * value;

        return Math.sqrt(norm);
    }

    private static class RescoringTask implements Callable<Void> {

        private final IndexReader reader;
        private final String fieldName;
        private final ScoreDoc target;
        private final HashMap<String, Float> referenceTerms;
        private final double referenceL2Norm;

        public RescoringTask(IndexReader reader, String fieldName,
                             ScoreDoc target, HashMap<String, Float> referenceTerms, double referenceL2Norm) {
            this.reader = reader;
            this.fieldName = fieldName;
            this.target = target;
            this.referenceTerms = referenceTerms;
            this.referenceL2Norm = referenceL2Norm;
        }

        @Override
        public Void call() throws IOException {
            HashMap<String, Float> terms = LuceneUtils.getTermFrequencies(this.reader, this.target.doc, this.fieldName);

            double dotProduct = 0;
            double l2Norm = 0;

            for (Float value : terms.values())
                l2Norm += value * value;
            l2Norm = Math.sqrt(l2Norm);

            for (Map.Entry<String, Float> entry : referenceTerms.entrySet()) {
                Float otherFreq = terms.get(entry.getKey());
                if (otherFreq != null)
                    dotProduct += entry.getValue() * otherFreq;
            }

            float similarity = (float) (dotProduct / (referenceL2Norm * l2Norm));

            if (Float.isInfinite(similarity) || Float.isNaN(similarity))
                this.target.score = 0.f;
            else
                this.target.score = similarity;

            return null;
        }
    }

}
