package eu.modernmt.decoder.neural.execution.impl;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.decoder.neural.execution.PythonDecoder;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

class Handler implements PythonDecoder {

    private final PythonDecoder.Builder builder;
    private final Map<LanguageDirection, File> checkpoints;
    private final int gpu;

    private PythonDecoder delegate = null;
    private File checkpoint = null;
    private boolean inUse;

    public Handler(Builder builder, Map<LanguageDirection, File> checkpoints, int gpu) {
        this.builder = builder;
        this.checkpoints = checkpoints;
        this.gpu = gpu;
        this.inUse = false;
    }

    public synchronized boolean setInUse() {
        if (this.inUse)
            return false;

        this.inUse = true;
        return true;
    }

    public synchronized boolean unsetInUse() {
        if (!this.inUse)
            return false;

        this.inUse = false;
        return true;
    }

    public void restart() throws IOException {
        IOUtils.closeQuietly(this);

        if (gpu < 0)
            delegate = builder.startOnCPU();
        else
            delegate = builder.startOnGPU(gpu);
    }

    public File getLastCheckpoint() {
        return checkpoint;
    }

    @Override
    public int getGPU() {
        return gpu;
    }

    @Override
    public boolean isAlive() {
        return delegate != null && delegate.isAlive();
    }

    @Override
    public synchronized Translation translate(LanguageDirection direction, Sentence sentence, int nBest) throws DecoderException {
        if (delegate == null)
            throw new DecoderUnavailableException("Decoder process is dead");

        checkpoint = checkpoints.get(direction);
        return delegate.translate(direction, sentence, nBest);
    }

    @Override
    public synchronized Translation translate(LanguageDirection direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws DecoderException {
        if (delegate == null)
            throw new DecoderUnavailableException("Decoder process is dead");

        checkpoint = checkpoints.get(direction);
        return delegate.translate(direction, sentence, suggestions, nBest);
    }

    @Override
    public Translation[] translate(LanguageDirection direction, Sentence[] sentences, int nBest) throws DecoderException {
        if (delegate == null)
            throw new DecoderUnavailableException("Decoder process is dead");

        checkpoint = checkpoints.get(direction);
        return delegate.translate(direction, sentences, nBest);
    }

    @Override
    public Translation[] translate(LanguageDirection direction, Sentence[] sentences, ScoreEntry[] suggestions, int nBest) throws DecoderException {
        if (delegate == null)
            throw new DecoderUnavailableException("Decoder process is dead");

        checkpoint = checkpoints.get(direction);
        return delegate.translate(direction, sentences, suggestions, nBest);
    }

    @Override
    public synchronized Translation align(LanguageDirection direction, Sentence sentence, String[] translation) throws DecoderException {
        if (delegate == null)
            throw new DecoderUnavailableException("Decoder process is dead");

        checkpoint = checkpoints.get(direction);
        return delegate.align(direction, sentence, translation);
    }

    @Override
    public void test() throws DecoderException {
        if (delegate == null)
            throw new DecoderUnavailableException("Decoder process is dead");
        delegate.test();
    }

    @Override
    public void close() throws IOException {
        checkpoint = null;

        if (delegate != null)
            delegate.close();
    }

}
