package eu.modernmt.decoder.neural.natv;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by davide on 05/06/17.
 * <p>
 * A NativeProcess represents a separate process that is launched by an MMT engine to run an NeuralDecoder.
 * The NativeProcess object is thus to run and request translations to its specific decoder process.
 * If necessary, it also handles its close.
 */
public interface NativeProcess extends Closeable {

    interface Builder {

        NativeProcess start(int gpu) throws IOException;

    }

    int getGPU();

    boolean isAlive();

    Translation translate(LanguagePair direction, Sentence sentence, int nBest) throws DecoderException;

    Translation translate(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws DecoderException;

}
