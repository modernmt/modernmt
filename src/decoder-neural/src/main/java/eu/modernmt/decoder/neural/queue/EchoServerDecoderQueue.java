package eu.modernmt.decoder.neural.queue;

import eu.modernmt.decoder.DecoderListener;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.lang.LanguageDirection;

import java.util.concurrent.TimeUnit;

public class EchoServerDecoderQueue implements DecoderQueue {

    @Override
    public PythonDecoder take(LanguageDirection language) {
        return EchoPythonDecoder.INSTANCE;
    }

    @Override
    public PythonDecoder poll(LanguageDirection language, long timeout, TimeUnit unit) {
        return EchoPythonDecoder.INSTANCE;
    }

    @Override
    public void release(PythonDecoder decoder) {
        // Nothing to do
    }

    @Override
    public int availability() {
        return 2;
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public void setListener(DecoderListener listener) {
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to do
    }

}
