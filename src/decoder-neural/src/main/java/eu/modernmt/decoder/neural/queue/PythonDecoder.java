package eu.modernmt.decoder.neural.queue;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
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

    Translation translate(LanguageDirection direction, Sentence sentence, int alternatives) throws DecoderException;

    Translation translate(LanguageDirection direction, Sentence sentence, ScoreEntry[] suggestions, int alternatives) throws DecoderException;

    Translation[] translate(LanguageDirection direction, Sentence[] sentences, int[] alternatives) throws DecoderException;

    Translation[] translate(LanguageDirection direction, Sentence[] sentences, ScoreEntry[] suggestions, int[] alternatives) throws DecoderException;

    Translation align(LanguageDirection direction, Sentence sentence, String[] translation) throws DecoderException;

    Translation[] align(LanguageDirection direction, Sentence[] sentences, String[][] translations) throws DecoderException;

    void test() throws DecoderException;

}
