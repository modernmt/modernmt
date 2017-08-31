package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.neural.NeuralDecoderException;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;

import java.io.IOException;

/**
 * Created by davide on 23/05/17.
 * <p>
 * A SingletonExecutionQueue launches and manages a single NeuralDecoder processe.
 * It assigns it translation jobs and, if necessary, closes it.
 */
class SingletonExecutionQueue implements ExecutionQueue {

    /**
     * This method launches a single NeuralDecoder process that must be run on CPU
     * and returns the NativeProcess object to interact with it.
     *
     * @param startTask a StartNativeProcessCpuTask to execute
     * @return the NativeProcess object resulting from the execution of the passed task
     * @throws NeuralDecoderException
     */
    public static SingletonExecutionQueue forCPU(StartNativeProcessCpuTask startTask) throws NeuralDecoderException {
        /*borderline case: if only one decoder process must be launched, execute the task in this thread*/
        try {
            return new SingletonExecutionQueue(startTask.call());
        } catch (IOException e) {
            throw new NeuralDecoderException("Unable to start NMT process", e);
        }
    }

    /**
     * This method launches a single NeuralDecoder process that must be run on GPU
     * and returns the NativeProcess object to interact with it.
     *
     * @param startTask a StartNativeProcessCpuTask to execute
     * @return the NativeProcess object resulting from the execution of the passed task
     * @throws NeuralDecoderException
     */
    public static SingletonExecutionQueue forGPU(StartNativeProcessGpuTask startTask) throws NeuralDecoderException {
        /*borderline case: if only one decoder process must be launched, execute the task in this thread*/
        try {
            return new SingletonExecutionQueue(startTask.call());
        } catch (IOException e) {
            throw new NeuralDecoderException("Unable to start NMT process", e);
        }
    }

    private final NativeProcess decoder;    // the decoder NativeProcess that this SingletonExecutionQueue manages

    private SingletonExecutionQueue(NativeProcess decoder) {
        this.decoder = decoder;
    }

    /**
     * This method assigns a translation job to the NeuralDecoder processe
     * that this SingletonExecutionQueue manages, and returns the translation result.
     *
     * @param direction the direction of the translation to execute
     * @param sentence  the source sentence to translate
     * @return a Translation object representing the translation result
     * @throws NeuralDecoderException
     */
    @Override
    public synchronized Translation execute(LanguagePair direction, Sentence sentence) throws NeuralDecoderException {
        Word[] translation = decoder.translate(direction, sentence);
        return new Translation(translation, sentence, null);
    }

    /**
     * This method assigns a translation job to the NeuralDecoder processe
     * that this SingletonExecutionQueue manages, and returns the translation result.
     *
     * @param direction   the direction of the translation to execute
     * @param sentence    the source sentence to translate
     * @param suggestions an array of translation suggestions that the decoder will study before the translation
     * @return a Translation object representing the translation result
     * @throws NeuralDecoderException
     */
    @Override
    public synchronized Translation execute(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions) throws NeuralDecoderException {
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
