package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 22/05/17.
 */
public interface ExecutionQueue extends Closeable {

    Logger logger = LogManager.getLogger(ExecutionQueue.class);

    static ExecutionQueue newInstance(File home, File model) throws OpenNMTException {
        try {
            int gpus = MultiGPUsExecutionQueue.getNumberOfGPUs();

            if (gpus < 1)
                logger.warn("Unable to retrieve number of GPUs, result is " + gpus);

            return newInstance(home, model, gpus);
        } catch (IOException e) {
            logger.error("Unable to retrieve number of GPUs, failed to execute command. Falling back to SingletonExecutionQueue", e);
            return newInstance(home, model, 1);
        }
    }

    static ExecutionQueue newInstance(File home, File model, int gpus) throws OpenNMTException {
        NativeProcess.Builder builder = new NativeProcess.Builder(home, model);

        if (gpus > 1)
            return new MultiGPUsExecutionQueue(builder, gpus);
        else
            return new SingletonExecutionQueue(builder);
    }

    Translation execute(Sentence sentence) throws OpenNMTException;

    Translation execute(Sentence sentence, ScoreEntry[] suggestions) throws OpenNMTException;

}
