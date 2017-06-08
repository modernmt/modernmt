package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;

import java.io.IOException;

/**
 * Created by davide on 23/05/17.
 */
class SingletonExecutionQueue implements ExecutionQueue {

    private final NativeProcess decoder;

    public SingletonExecutionQueue(NativeProcess.Builder builder) throws OpenNMTException {
        try {
            this.decoder = builder.start();
        } catch (IOException e) {
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        }
    }

    @Override
    public synchronized Translation execute(Sentence sentence) throws OpenNMTException {
        Word[] translation = decoder.translate(sentence);
        return new Translation(translation, sentence, null);
    }

    @Override
    public synchronized Translation execute(Sentence sentence, ScoreEntry[] suggestions) throws OpenNMTException {
        Word[] translation = decoder.translate(sentence, suggestions);
        return new Translation(translation, sentence, null);
    }

    @Override
    public void close() throws IOException {
        decoder.close();
    }

}
