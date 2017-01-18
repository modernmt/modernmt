package eu.modernmt.facade.operations;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.engine.Engine;
import eu.modernmt.model.ContextVector;

import java.io.File;

/**
 * Created by davide on 22/04/16.
 */
public class GetContextOperation extends Operation<ContextVector> {

    private final File file;
    private final int limit;
    private final String text;

    public GetContextOperation(File file, int limit) {
        this.file = file;
        this.text = null;
        this.limit = limit;
    }

    public GetContextOperation(String text, int limit) {
        this.file = null;
        this.text = text;
        this.limit = limit;
    }

    @Override
    public ContextVector call() throws ContextAnalyzerException {
        Engine engine = getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        return (file == null) ? analyzer.getContextVector(text, limit) : analyzer.getContextVector(file, limit);
    }

}
