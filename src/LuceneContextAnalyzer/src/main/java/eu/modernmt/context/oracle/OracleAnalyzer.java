package eu.modernmt.context.oracle;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.model.Corpus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by david on 09/05/16.
 */
public class OracleAnalyzer extends ContextAnalyzer implements AutoCloseable {

    private final Logger logger = LogManager.getLogger(ContextAnalyzer.class);

    public OracleAnalyzer() {
    }

    @Override
    public void rebuild(Collection<? extends Corpus> documents) throws ContextAnalyzerException {
    }

    @Override
    public List<ContextDocument> getContext(Corpus query, int limit) throws ContextAnalyzerException {
        // return 1.0 score for the training domain of the same name as the one queried.
        // this only works if the training and dev/test domains are identically named.

        List<ContextDocument> l = new ArrayList<ContextDocument>();
        l.add(new ContextDocument(query.getName(), 1.0f));
        return l;
    }

    @Override
    public void close() throws IOException {
    }
}
