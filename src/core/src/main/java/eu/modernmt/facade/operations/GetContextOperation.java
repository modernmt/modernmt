package eu.modernmt.facade.operations;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextScore;
import eu.modernmt.engine.Engine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 22/04/16.
 */
public class GetContextOperation extends Operation<ArrayList<ContextScore>> {

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
    public ArrayList<ContextScore> call() throws ContextAnalyzerException {
        Engine engine = getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        List<ContextScore> result = (file == null) ? analyzer.getContext(text, limit) : analyzer.getContext(file, limit);

        if (result instanceof ArrayList)
            return (ArrayList<ContextScore>) result;
        else
            return new ArrayList<>(result);
    }

}
