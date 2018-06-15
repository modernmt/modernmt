package eu.modernmt.context.lucene.storage;

import eu.modernmt.context.lucene.analysis.ContextAnalyzerIndex;
import eu.modernmt.context.lucene.analysis.DocumentBuilder;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.LanguagePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.Callable;

/**
 * Created by davide on 23/09/16.
 */
class AnalysisTask implements Callable<Void> {

    private final Logger logger = LogManager.getLogger(AnalysisTask.class);

    private final ContextAnalyzerIndex index;
    private final CorpusBucket bucket;

    public AnalysisTask(ContextAnalyzerIndex index, CorpusBucket bucket) {
        this.index = index;
        this.bucket = bucket;
    }

    @Override
    public Void call() throws IOException {
        String id = bucket.getDocumentId();

        logger.info("Indexing bucket " + bucket);

        try {
            Reader reader = new InputStreamReader(bucket.getContentStream(), UTF8Charset.get());
            Document document = DocumentBuilder.updatedInstance(bucket.getOwner(), id, reader);
            index.update(document);

            bucket.onAnalysisCompleted();
        } catch (FileNotFoundException e) {
            // Missing file? Ignore it!
        }

        return null;
    }
}
