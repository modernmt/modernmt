package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.model.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;

import java.io.IOException;

/**
 * Created by davide on 23/05/17.
 */
class SingletonExecutionQueue implements ExecutionQueue {

    private final NativeProcess decoder;

    public static SingletonExecutionQueue forCPU(NativeProcess.Builder builder) throws OpenNMTException {
        try {
            return new SingletonExecutionQueue(builder.startOnCPU());
        } catch (IOException e) {
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        }
    }

    public static SingletonExecutionQueue forGPU(NativeProcess.Builder builder, int gpu) throws OpenNMTException {
        try {
            return new SingletonExecutionQueue(builder.startOnGPU(gpu));
        } catch (IOException e) {
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        }
    }

    private SingletonExecutionQueue(NativeProcess decoder) {
        this.decoder = decoder;
    }

    @Override
    public synchronized Translation execute(LanguagePair direction, Sentence sentence) throws OpenNMTException {
        Word[] translation = decoder.translate(direction, sentence);
        return new Translation(translation, sentence, null);
    }

    @Override
    public synchronized Translation execute(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions) throws OpenNMTException {
        Word[] translation = decoder.translate(direction, sentence, suggestions);
        return new Translation(translation, sentence, null);
    }

    @Override
    public void close() throws IOException {
        decoder.close();
    }

}
