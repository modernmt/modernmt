package eu.modernmt.processing.detokenizer;

import eu.modernmt.processing.framework.ProcessingException;
import org.apache.commons.io.IOUtils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by davide on 26/01/16.
 */
public class MultiInstanceDetokenizer implements Detokenizer {

    public interface DetokenizerFactory {

        Detokenizer newInstance();

    }

    private Queue<Detokenizer> buffer = new ConcurrentLinkedQueue<>();
    private DetokenizerFactory factory;
    private boolean closed = false;

    public MultiInstanceDetokenizer(DetokenizerFactory factory) {
        this.factory = factory;
    }

    private Detokenizer getInstance() {
        Detokenizer instance = buffer.poll();

        if (instance == null)
            instance = factory.newInstance();

        return instance;
    }

    private void releaseInstance(Detokenizer detokenizer) {
        boolean added = false;

        synchronized (this) {
            if (!closed)
                added = buffer.add(detokenizer);
        }

        if (!added)
            IOUtils.closeQuietly(detokenizer);
    }

    @Override
    public String call(String[] param) throws ProcessingException {
        Detokenizer instance = getInstance();

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

        for (Detokenizer detokenizer : buffer)
            IOUtils.closeQuietly(detokenizer);
    }

}
