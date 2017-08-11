package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;

import java.io.IOException;

/**
 * Created by davide on 23/05/17.
 */
class SingletonExecutionQueue implements ExecutionQueue {

    public static SingletonExecutionQueue forCPU(StartNativeProcessCpuTask startTask) throws OpenNMTException {
        /*borderline case: if only one decoder process must be launched, execute the task in this thread*/
        try {
            return new SingletonExecutionQueue(startTask.call());
        } catch (IOException e) {
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        }
    }

    public static SingletonExecutionQueue forGPU(StartNativeProcessGpuTask startTask) throws OpenNMTException {
        /*borderline case: if only one decoder process must be launched, execute the task in this thread*/
        try {
            return new SingletonExecutionQueue(startTask.call());
        } catch (IOException e) {
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        }
    }

    private final NativeProcess decoder;

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
