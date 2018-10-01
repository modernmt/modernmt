package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.io.Closeable;
import java.io.IOException;

public interface PythonDecoder extends Closeable {

    interface Builder {

        PythonDecoder startOnCPU() throws IOException;

        PythonDecoder startOnGPU(int gpu) throws IOException;

    }

    int getGPU();

    boolean isAlive();

    Translation translate(LanguagePair direction, Sentence sentence, int nBest) throws DecoderException;

    Translation translate(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws DecoderException;

    Translation translate(LanguagePair direction, Sentence sentence, String[] translation) throws DecoderException;

    void test() throws DecoderException;

}
