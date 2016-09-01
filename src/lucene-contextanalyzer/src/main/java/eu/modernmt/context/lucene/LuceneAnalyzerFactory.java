package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextAnalyzerFactory;
import eu.modernmt.io.Paths;

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 09/05/16.
 */
public class LuceneAnalyzerFactory extends ContextAnalyzerFactory {

    @Override
    public ContextAnalyzer create() throws ContextAnalyzerException {
        File indexPath = Paths.join(enginePath, "models", "context", "index");
        try {
            return new LuceneAnalyzer(indexPath);
        } catch (IOException e) {
            throw new ContextAnalyzerException("Unable to load context analyzer from path " + indexPath, e);
        }
    }

}
