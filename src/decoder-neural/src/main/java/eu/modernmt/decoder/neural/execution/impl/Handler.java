package eu.modernmt.decoder.neural.execution.impl;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.decoder.neural.execution.PythonDecoder;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

class Handler implements PythonDecoder {

    private final PythonDecoder.Builder builder;
    private final Map<LanguagePair, File> checkpoints;
    private final int gpu;

    private PythonDecoder delegate = null;
    private File checkpoint = null;

    public Handler(Builder builder, Map<LanguagePair, File> checkpoints, int gpu) {
        this.builder = builder;
        this.checkpoints = checkpoints;
        this.gpu = gpu;
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
    public synchronized Translation translate(LanguagePair direction, Sentence sentence, int nBest) throws DecoderException {
        if (delegate == null)
            throw new DecoderUnavailableException("Decoder process is dead");

        checkpoint = checkpoints.get(direction);
        return delegate.translate(direction, sentence, nBest);
    }

    @Override
    public synchronized Translation translate(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws DecoderException {
        if (delegate == null)
            throw new DecoderUnavailableException("Decoder process is dead");

        checkpoint = checkpoints.get(direction);
        return delegate.translate(direction, sentence, suggestions, nBest);
    }

    @Override
    public void close() throws IOException {
        checkpoint = null;

        if (delegate != null)
            delegate.close();
    }

}
