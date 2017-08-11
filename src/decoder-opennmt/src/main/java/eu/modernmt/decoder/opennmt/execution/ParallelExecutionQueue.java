package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import org.apache.commons.io.IOUtils;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by davide on 23/05/17.
 */
class ParallelExecutionQueue implements ExecutionQueue {

    public static ParallelExecutionQueue forCPUs(ArrayList<StartNativeProcessCpuTask> tasks) throws OpenNMTException {
        return executeStartTasks(tasks);
    }

    public static ParallelExecutionQueue forGPUs(ArrayList<StartNativeProcessGpuTask> tasks) throws OpenNMTException {
        return executeStartTasks(tasks);
    }

    private static ParallelExecutionQueue executeStartTasks(ArrayList<? extends StartNativeProcessTask> tasks) throws OpenNMTException {
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

    private static NativeProcess[] getProcesses(ArrayList<Future<NativeProcess>> futures) throws OpenNMTException {
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
                logger.error("Unable to start OpenNMT process", e);
            }
        }

        if (!success) {
            for (NativeProcess process : processes)
                IOUtils.closeQuietly(process);
            throw new OpenNMTException("Unable to start OpenNMT process");
        }

        return processes;
    }


    private final NativeProcess[] processes;
    private final ArrayBlockingQueue<NativeProcess> queue;

    private ParallelExecutionQueue(NativeProcess[] processes) {
        this.processes = processes;
        this.queue = new ArrayBlockingQueue<>(processes.length);

        for (NativeProcess process : processes)
            this.queue.offer(process);
    }

    @Override
    public Translation execute(LanguagePair direction, Sentence sentence) throws OpenNMTException {
        return execute(direction, sentence, null);
    }

    @Override
    public Translation execute(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions) throws
            OpenNMTException {
        NativeProcess decoder = null;

        try {
            decoder = this.queue.take();

            Word[] translation = decoder.translate(direction, sentence, suggestions);
            return new Translation(translation, sentence, null);
        } catch (InterruptedException e) {
            throw new OpenNMTException("No OpenNMT processes available", e);
        } finally {
            if (decoder != null)
                this.queue.offer(decoder);
        }
    }

    @Override
    public void close() {
        for (NativeProcess decoder : processes)
            IOUtils.closeQuietly(decoder);
    }
}
