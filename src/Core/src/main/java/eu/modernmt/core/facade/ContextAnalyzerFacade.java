package eu.modernmt.core.facade;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.core.cluster.error.SystemShutdownException;
import eu.modernmt.core.facade.operations.GetContextOperation;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 20/04/16.
 */
public class ContextAnalyzerFacade {

    public List<ContextDocument> get(File context, int limit) throws ContextAnalyzerException {
        GetContextOperation operation = new GetContextOperation(context, limit);

        try {
            return ModernMT.client.submit(operation).get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException();
        } catch (ExecutionException e) {
            throw unwrap(e);
        }
    }

    public List<ContextDocument> get(String context, int limit) throws ContextAnalyzerException {
        GetContextOperation operation = new GetContextOperation(context, limit);

        try {
            return ModernMT.client.submit(operation).get();
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
            return new ContextAnalyzerException("Unexpected error in context analyzer", cause);
        else
            throw new Error("Unexpected exception: " + cause.getMessage(), cause);
    }

}
