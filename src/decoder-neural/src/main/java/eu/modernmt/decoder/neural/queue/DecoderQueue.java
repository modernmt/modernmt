package eu.modernmt.decoder.neural.queue;

import eu.modernmt.decoder.DecoderListener;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.lang.LanguageDirection;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

public interface DecoderQueue extends Closeable {

    PythonDecoder take(LanguageDirection language) throws DecoderUnavailableException;

    PythonDecoder poll(LanguageDirection language, long timeout, TimeUnit unit) throws DecoderUnavailableException;

    void release(PythonDecoder decoder);

    int availability();

    int size();

    void setListener(DecoderListener listener);

}
