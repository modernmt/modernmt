package eu.modernmt.data;

import eu.modernmt.model.ImportJob;
import eu.modernmt.model.Memory;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.TranslationUnit;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 06/09/16.
 */
public interface BinaryLog extends Closeable {

    short MEMORY_UPLOAD_CHANNEL_ID = 0;
    short CONTRIBUTIONS_CHANNEL_ID = 1;

    interface Listener {
        void onLogDataBatchProcessed(Map<Short, Long> updatedPositions);
    }

    Map<Short, Long> connect() throws HostUnreachableException;

    Map<Short, Long> connect(long timeout, TimeUnit unit) throws HostUnreachableException;

    Map<Short, Long> connect(long timeout, TimeUnit unit, boolean enableConsumer, boolean enableProducer) throws HostUnreachableException;

    void setBinaryLogListener(Listener listener);

    void addLogDataListener(LogDataListener listener);

    ImportJob upload(Memory memory, MultilingualCorpus corpus, short channel) throws BinaryLogException;

    ImportJob upload(Memory memory, MultilingualCorpus corpus, LogChannel channel) throws BinaryLogException;

    ImportJob upload(Memory memory, TranslationUnit tu, short channel) throws BinaryLogException;

    ImportJob upload(Memory memory, TranslationUnit tu, LogChannel channel) throws BinaryLogException;

    ImportJob replace(Memory memory, TranslationUnit tu, short channel) throws BinaryLogException;

    ImportJob replace(Memory memory, TranslationUnit tu, LogChannel channel) throws BinaryLogException;

    ImportJob replace(Memory memory, TranslationUnit tu, String previousSentence, String previousTranslation, short channel) throws BinaryLogException;

    ImportJob replace(Memory memory, TranslationUnit tu, String previousSentence, String previousTranslation, LogChannel channel) throws BinaryLogException;

    void delete(Memory memory) throws BinaryLogException;

    LogChannel getLogChannel(short id);

    Map<Short, Long> getChannelsPositions();

    void waitChannelPosition(short channel, long position) throws InterruptedException;

    void waitChannelPositions(Map<Short, Long> positions) throws InterruptedException;

}
