package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.neural.NeuralDecoderException;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import org.apache.commons.io.IOUtils;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by davide on 23/05/17.
 * <p>
 * A ParallelExecutionQueue launches and manages a group of NeuralDecoder processes.
 * It assigns them translation jobs and, if necessary, closes the processes.
 */
class ParallelExecutionQueue implements ExecutionQueue {

    /**
     * This method launches multiple NeuralDecoder processes that must be run on CPU
     * and returns the list of NativeProcess objects to interact with them.
     *
     * @param tasks a list of StartNativeProcessCpuTask to execute
     * @return the list of NativeProcess object resulting from the execution of all the passed tasks
     * @throws NeuralDecoderException
     */
    public static ParallelExecutionQueue forCPUs(ArrayList<StartNativeProcessCpuTask> tasks) throws NeuralDecoderException {
        return executeStartTasks(tasks);
    }

    /**
     * This method launches multiple NeuralDecoder processes that must be run on GPU
     * and returns the list of NativeProcess objects to interact with them.
     *
     * @param tasks a list of StartNativeProcessGpuTask to execute
     * @return the list of NativeProcess object resulting from the execution of all the passed tasks
     */
    public static ParallelExecutionQueue forGPUs(ArrayList<StartNativeProcessGpuTask> tasks) throws NeuralDecoderException {
        return executeStartTasks(tasks);
    }

    private static ParallelExecutionQueue executeStartTasks(ArrayList<? extends StartNativeProcessTask> tasks) throws NeuralDecoderException {
        ExecutorService executor;
        ArrayList<Future<NativeProcess>> futures;

        /*start decoder processes using GPUs*/
        futures = new ArrayList<>(tasks.size());
        executor = Executors.newFixedThreadPool(tasks.size());
        for (int i = 0; i < tasks.size(); i++)
            futures.add(i, executor.submit(tasks.get(i)));
        executor.shutdown();
        NativeProcess[] processes = getProcesses(futures);
        return new ParallelExecutionQueue(processes);
    }

    private static NativeProcess[] getProcesses(ArrayList<Future<NativeProcess>> futures) throws NeuralDecoderException {
        NativeProcess[] processes = new NativeProcess[futures.size()];
        boolean success = true;

        /*get all the NativeProcesses for all the futures.
        * if an exception is thrown, mark that something has gone wrong
        * and keep getting the processes (so it will be possible to stop them all later)*/
        for (int i = 0; i < futures.size(); i++) {
            try {
                processes[i] = futures.get(i).get();
            } catch (Exception e) {
                success = false;
                logger.error("Unable to start NMT process", e);
            }
        }

        if (!success) {
            for (NativeProcess process : processes)
                IOUtils.closeQuietly(process);
            throw new NeuralDecoderException("Unable to start NMT process");
        }

        return processes;
    }


    private final NativeProcess[] processes;    //the list of decoder NativeProcesses to manage
    private final ArrayBlockingQueue<NativeProcess> queue;  //queue of NativeProcesses allowing round-robin access

    private ParallelExecutionQueue(NativeProcess[] processes) {
        this.processes = processes;
        this.queue = new ArrayBlockingQueue<>(processes.length);

        for (NativeProcess process : processes)
            this.queue.offer(process);
    }

    @Override
    public Translation execute(LanguagePair direction, Sentence sentence, int nBest) throws NeuralDecoderException {
        return execute(direction, sentence, null, nBest);
    }

    @Override
    public Translation execute(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws NeuralDecoderException {
        NativeProcess decoder = null;

        try {
            decoder = this.queue.take();
            return decoder.translate(direction, sentence, suggestions, nBest);
        } catch (InterruptedException e) {
            throw new NeuralDecoderException("No NMT processes available", e);
        } finally {
            if (decoder != null)
                this.queue.offer(decoder);
        }
    }

    /**
     * This method closes all the decoder processes that this ParallelExecutionQueue manages
     */
    @Override
    public void close() {
        for (NativeProcess decoder : processes)
            IOUtils.closeQuietly(decoder);
    }
}
