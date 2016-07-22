package eu.modernmt.facade.operations;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.Engine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 22/04/16.
 */
public class GetContextOperation extends Operation<ArrayList<ContextDocument>> {

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
    public ArrayList<ContextDocument> call() throws ContextAnalyzerException {
        Engine engine = getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();
        Locale lang = engine.getSourceLanguage();

        List<ContextDocument> result = (file == null) ? analyzer.getContext(text, lang, limit) : analyzer.getContext(file, lang, limit);

        if (result instanceof ArrayList)
            return (ArrayList<ContextDocument>) result;
        else
            return new ArrayList<>(result);
    }

}
