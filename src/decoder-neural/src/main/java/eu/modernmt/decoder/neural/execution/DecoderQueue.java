package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.DecoderListener;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.lang.LanguagePair;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

public interface DecoderQueue extends Closeable {

    PythonDecoder take(LanguagePair language) throws DecoderUnavailableException;

    PythonDecoder poll(LanguagePair language, long timeout, TimeUnit unit) throws DecoderUnavailableException;

    void release(PythonDecoder decoder);

    int availability();

    void setListener(DecoderListener listener);

}
