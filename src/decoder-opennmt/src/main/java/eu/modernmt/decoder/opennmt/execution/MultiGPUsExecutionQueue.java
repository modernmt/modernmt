package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by davide on 23/05/17.
 */
class MultiGPUsExecutionQueue implements ExecutionQueue {

    public static int getNumberOfGPUs() throws IOException {
        Process nvidia = Runtime.getRuntime().exec(new String[]{"nvidia-smi", "--list-gpus"});

        try {
            if (!nvidia.waitFor(10, TimeUnit.SECONDS))
                throw new IOException("Process \"nvidia-smi\" timeout");
        } catch (InterruptedException e) {
            throw new IOException("Process \"nvidia-smi\" timeout", e);
        } finally {
            if (nvidia.isAlive())
                nvidia.destroyForcibly();
        }

        final Pattern regex = Pattern.compile("^GPU [0-9]+:");

        int gpu = 0;
        for (String line : IOUtils.readLines(nvidia.getInputStream())) {
            line = line.trim();

            if (regex.matcher(line).find())
                gpu++;
        }

        return gpu;
    }

    private final NativeProcess[] processes;
    private final ArrayBlockingQueue<NativeProcess> queue;

    public MultiGPUsExecutionQueue(NativeProcess.Builder builder, int gpus) throws OpenNMTException {
        this.processes = new NativeProcess[gpus];
        this.queue = new ArrayBlockingQueue<>(gpus);

        boolean success = false;
        try {
            for (int i = 0; i < gpus; i++) {
                NativeProcess process = builder.start();
                this.processes[i] = process;
                this.queue.offer(process);
            }

            success = true;
        } catch (IOException e) {
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        } finally {
            if (!success)
                close();
        }
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

            Word[] translation = decoder.translate(sentence);
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
