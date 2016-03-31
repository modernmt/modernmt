package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.string.XMLEditableString;
import org.apache.commons.io.IOUtils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by davide on 26/01/16.
 */
public class MultiInstanceTokenizer implements Tokenizer {

    public interface TokenizerFactory {

        Tokenizer newInstance();

    }

    private Queue<Tokenizer> buffer = new ConcurrentLinkedQueue<>();
    private TokenizerFactory factory;
    private boolean closed = false;

    public MultiInstanceTokenizer(TokenizerFactory factory) {
        this.factory = factory;
    }

    private Tokenizer getInstance() {
        Tokenizer instance = buffer.poll();

        if (instance == null)
            instance = factory.newInstance();

        return instance;
    }

    private void releaseInstance(Tokenizer detokenizer) {
        boolean added = false;

        synchronized (this) {
            if (!closed)
                added = buffer.add(detokenizer);
        }

        if (!added)
            IOUtils.closeQuietly(detokenizer);
    }

    @Override
    public XMLEditableString call(XMLEditableString param) throws ProcessingException {
        Tokenizer instance = getInstance();

        try {
            return instance.call(param);
        } finally {
            releaseInstance(instance);
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            closed = true;
        }

        buffer.forEach(IOUtils::closeQuietly);
    }

}
