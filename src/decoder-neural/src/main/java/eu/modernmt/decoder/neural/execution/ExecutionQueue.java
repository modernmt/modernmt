package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.neural.NeuralDecoderException;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by davide on 22/05/17.
 */
public interface ExecutionQueue extends Closeable {

    Logger logger = LogManager.getLogger(ExecutionQueue.class);

    static ExecutionQueue newGPUInstance(File home, File model, int[] gpus) throws NeuralDecoderException {
        if (gpus.length > 1) {
            ArrayList<StartNativeProcessGpuTask> startTasks = new ArrayList<>();
            for (int i = 0; i < gpus.length; i++)
                startTasks.add(i, new StartNativeProcessGpuTask(home, model, gpus[i]));
            return ParallelExecutionQueue.forGPUs(startTasks);
        } else {
            return SingletonExecutionQueue.forGPU(new StartNativeProcessGpuTask(home, model, gpus[0]));
        }
    }

    static ExecutionQueue newCPUInstance(File home, File model, int cpus) throws NeuralDecoderException {
        if (cpus > 1) {
            ArrayList<StartNativeProcessCpuTask> startTasks = new ArrayList<>();
            for (int i = 0; i < cpus; i++)
                startTasks.add(i, new StartNativeProcessCpuTask(home, model));
            return ParallelExecutionQueue.forCPUs(startTasks);
        } else {
            return SingletonExecutionQueue.forCPU(new StartNativeProcessCpuTask(home, model));
        }
    }


    Translation execute(LanguagePair direction, Sentence sentence, int nBest) throws NeuralDecoderException;

    Translation execute(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions, int nBest) throws NeuralDecoderException;

}
