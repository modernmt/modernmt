package eu.modernmt.context.lucene.storage;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.lucene.ContextAnalyzerIndex;
import eu.modernmt.context.lucene.DocumentBuilder;
import eu.modernmt.io.DefaultCharset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;

import java.io.FileNotFoundException;
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
    public Void call() throws ContextAnalyzerException {
        int domain = bucket.getDomain();

        logger.info("Indexing bucket " + domain);

        try {
            Reader reader = new InputStreamReader(bucket.getContentStream(), DefaultCharset.get());

            Document document = DocumentBuilder.createDocument(domain, reader);
            index.update(domain, document);

            bucket.onAnalysisCompleted();
        } catch (FileNotFoundException e) {
            // Missing file? Ignore it!
        }

        return null;
    }
}
