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
 * <p>
 * A SingletonExecutionQueue launches and manages a single OpenNMTDecoder processe.
 * It assigns it translation jobs and, if necessary, closes it.
 */
class SingletonExecutionQueue implements ExecutionQueue {

    /**
     * This method launches a single OpenNMTDecoder process that must be run on CPU
     * and returns the NativeProcess object to interact with it.
     *
     * @param startTask a StartNativeProcessCpuTask to execute
     * @return the NativeProcess object resulting from the execution of the passed task
     * @throws OpenNMTException
     */
    public static SingletonExecutionQueue forCPU(StartNativeProcessCpuTask startTask) throws OpenNMTException {
        /*borderline case: if only one decoder process must be launched, execute the task in this thread*/
        try {
            return new SingletonExecutionQueue(startTask.call());
        } catch (IOException e) {
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        }
    }

    /**
     * This method launches a single OpenNMTDecoder process that must be run on GPU
     * and returns the NativeProcess object to interact with it.
     *
     * @param startTask a StartNativeProcessCpuTask to execute
     * @return the NativeProcess object resulting from the execution of the passed task
     * @throws OpenNMTException
     */
    public static SingletonExecutionQueue forGPU(StartNativeProcessGpuTask startTask) throws OpenNMTException {
        /*borderline case: if only one decoder process must be launched, execute the task in this thread*/
        try {
            return new SingletonExecutionQueue(startTask.call());
        } catch (IOException e) {
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        }
    }

    private final NativeProcess decoder;    // the decoder NativeProcess that this SingletonExecutionQueue manages

    private SingletonExecutionQueue(NativeProcess decoder) {
        this.decoder = decoder;
    }

    /**
     * This method assigns a translation job to the OpenNMTDecoder processe
     * that this SingletonExecutionQueue manages, and returns the translation result.
     *
     * @param direction the direction of the translation to execute
     * @param sentence  the source sentence to translate
     * @return a Translation object representing the translation result
     * @throws OpenNMTException
     */
    @Override
    public synchronized Translation execute(LanguagePair direction, Sentence sentence) throws OpenNMTException {
        Word[] translation = decoder.translate(direction, sentence);
        return new Translation(translation, sentence, null);
    }

    /**
     * This method assigns a translation job to the OpenNMTDecoder processe
     * that this SingletonExecutionQueue manages, and returns the translation result.
     *
     * @param direction   the direction of the translation to execute
     * @param sentence    the source sentence to translate
     * @param suggestions an array of translation suggestions that the decoder will study before the translation
     * @return a Translation object representing the translation result
     * @throws OpenNMTException
     */
    @Override
    public synchronized Translation execute(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions) throws OpenNMTException {
        Word[] translation = decoder.translate(direction, sentence, suggestions);
        return new Translation(translation, sentence, null);
    }

    /**
     * This method closes the decoder process that this ParallelExecutionQueue manages
     */
    @Override
    public void close() throws IOException {
        decoder.close();
    }

}
