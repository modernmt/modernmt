package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by davide on 23/05/17.
 */
class ParallelExecutionQueue implements ExecutionQueue {

    private final NativeProcess[] processes;
    private final ArrayBlockingQueue<NativeProcess> queue;

    public static ParallelExecutionQueue forCPUs(NativeProcess.Builder builder, int cpus) throws OpenNMTException {
        NativeProcess[] processes = new NativeProcess[cpus];

        boolean success = false;
        try {
            for (int i = 0; i < cpus; i++)
                processes[i] = builder.startOnCPU();

            success = true;
        } catch (IOException e) {
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        } finally {
            if (!success) {
                for (NativeProcess decoder : processes)
                    IOUtils.closeQuietly(decoder);
            }
        }

        return new ParallelExecutionQueue(processes);
    }

    public static ParallelExecutionQueue forGPUs(NativeProcess.Builder builder, int[] gpus) throws OpenNMTException {
        NativeProcess[] processes = new NativeProcess[gpus.length];

        boolean success = false;
        try {
            for (int i = 0; i < gpus.length; i++)
                processes[i] = builder.startOnGPU(gpus[i]);

            success = true;
        } catch (IOException e) {
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        } finally {
            if (!success) {
                for (NativeProcess decoder : processes)
                    IOUtils.closeQuietly(decoder);
            }
        }

        return new ParallelExecutionQueue(processes);
    }

    private ParallelExecutionQueue(NativeProcess[] processes) {
        this.processes = processes;
        this.queue = new ArrayBlockingQueue<>(processes.length);

        for (NativeProcess process : processes)
            this.queue.offer(process);
    }

    @Override
    public Translation execute(Sentence sentence) throws OpenNMTException {
        return execute(sentence, null);
    }

    @Override
    public Translation execute(Sentence sentence, ScoreEntry[] suggestions) throws OpenNMTException {
        NativeProcess decoder = null;

        try {
            decoder = this.queue.take();

            Word[] translation = decoder.translate(sentence, suggestions);
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
