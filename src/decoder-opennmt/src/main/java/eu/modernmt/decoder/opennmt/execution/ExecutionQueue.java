package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
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

    static ExecutionQueue newInstance(File home, File model, int[] gpus) throws OpenNMTException {


//        if (gpus == null || gpus.length == 0) {
            ArrayList<StartNativeProcessCpuTask> startTasks = new ArrayList<>();
            int cpus = Runtime.getRuntime().availableProcessors();
            if (cpus > 1) {
                for (int i = 0; i < cpus; i++)
                    startTasks.add(i, new StartNativeProcessCpuTask(home, model));
                return ParallelExecutionQueue.forCPUs(startTasks);
            } else {
                return SingletonExecutionQueue.forCPU(new StartNativeProcessCpuTask(home, model));
            }
//        } else {
//            if (gpus.length > 1) {
//                ArrayList<StartNativeProcessGpuTask> startTasks = new ArrayList<>();
//                for (int i = 0; i < gpus.length; i++)
//                    startTasks.add(i, new StartNativeProcessGpuTask(home, model, i));
//                return ParallelExecutionQueue.forGPUs(startTasks);
//            } else {
//                return SingletonExecutionQueue.forGPU(new StartNativeProcessGpuTask(home, model, 0));
//            }
//        }

    }

    Translation execute(LanguagePair direction, Sentence sentence) throws OpenNMTException;

    Translation execute(LanguagePair direction, Sentence sentence, ScoreEntry[] suggestions) throws
            OpenNMTException;

}
