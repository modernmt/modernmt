package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.model.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;

/**
 * Created by davide on 22/05/17.
 */
public interface ExecutionQueue extends Closeable {

    Logger logger = LogManager.getLogger(ExecutionQueue.class);

    static ExecutionQueue newInstance(File home, File model, int[] gpus) throws OpenNMTException {
        NativeProcess.Builder builder = new NativeProcess.Builder(home, model);

        if (gpus == null || gpus.length == 0) {
            int cpus = Runtime.getRuntime().availableProcessors();

            if (cpus > 1)
                return ParallelExecutionQueue.forCPUs(builder, cpus);
            else
                return SingletonExecutionQueue.forCPU(builder);
        } else {
            if (gpus.length > 1)
                return ParallelExecutionQueue.forGPUs(builder, gpus);
            else
                return SingletonExecutionQueue.forGPU(builder, gpus[0]);
        }
    }

    Translation execute(LanguagePair direction, Sentence sentence) throws OpenNMTException;

    Translation execute(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions) throws OpenNMTException;

}
