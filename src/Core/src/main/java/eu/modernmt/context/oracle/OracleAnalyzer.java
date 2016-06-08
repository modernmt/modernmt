package eu.modernmt.context.oracle;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.model.Corpus;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by david on 09/05/16.
 */
public class OracleAnalyzer extends ContextAnalyzer implements AutoCloseable {

    public OracleAnalyzer() {
    }

    @Override
    public void rebuild(Collection<? extends Corpus> documents) throws ContextAnalyzerException {
    }

    @Override
    public List<ContextDocument> getContext(Corpus query, int limit) throws ContextAnalyzerException {
        // return 1.0 score for the training domain of the same name as the one queried.
        // this only works if the training and dev/test domains are identically named.

        return Collections.singletonList(new ContextDocument(query.getName(), 1.0f));
    }

    @Override
    public void close() throws IOException {
    }
}
