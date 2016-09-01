package eu.modernmt.facade;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.engine.Engine;
import eu.modernmt.cluster.error.SystemShutdownException;
import eu.modernmt.facade.operations.GetContextOperation;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 20/04/16.
 */
public class ContextAnalyzerFacade {

    public List<ContextDocument> get(File context, int limit) throws ContextAnalyzerException {
        // Because the file is local to the machine, this method ensures that the
        // local context analyzer is invoked instead of a remote one
        Engine engine = ModernMT.node.getEngine();
        Locale lang = engine.getSourceLanguage();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        return analyzer.getContext(context, lang, limit);
    }

    public List<ContextDocument> get(String context, int limit) throws ContextAnalyzerException {
        GetContextOperation operation = new GetContextOperation(context, limit);

        try {
            return ModernMT.node.submit(operation).get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException();
        } catch (ExecutionException e) {
            throw unwrap(e);
        }
    }

    private static ContextAnalyzerException unwrap(ExecutionException e) {
        Throwable cause = e.getCause();

        if (cause instanceof ContextAnalyzerException)
            return new ContextAnalyzerException("Problem in context analyzer", cause);
        else if (cause instanceof RuntimeException)
            return new ContextAnalyzerException("Unexpected exceptions in context analyzer", cause);
        else
            throw new Error("Unexpected exception: " + cause.getMessage(), cause);
    }

}
