package eu.modernmt.data;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ImportJob;
import eu.modernmt.model.Memory;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.Closeable;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 06/09/16.
 */
public interface DataManager extends Closeable {

    short MEMORY_UPLOAD_CHANNEL_ID = 0;
    short CONTRIBUTIONS_CHANNEL_ID = 1;

    interface Listener {
        void onDataBatchProcessed(Map<Short, Long> updatedPositions);
    }

    Map<Short, Long> connect() throws HostUnreachableException;

    Map<Short, Long> connect(long timeout, TimeUnit unit) throws HostUnreachableException;

    Map<Short, Long> connect(long timeout, TimeUnit unit, boolean enableConsumer, boolean enableProducer) throws HostUnreachableException;

    void setDataManagerListener(Listener listener);

    void addDataListener(DataListener listener);

    ImportJob upload(Memory memory, MultilingualCorpus corpus, short channel) throws DataManagerException;

    ImportJob upload(Memory memory, MultilingualCorpus corpus, DataChannel channel) throws DataManagerException;

    ImportJob upload(LanguagePair direction, Memory memory, String sentence, String translation, Date timestamp, short channel) throws DataManagerException;

    ImportJob upload(LanguagePair direction, Memory memory, String sentence, String translation, Date timestamp, DataChannel channel) throws DataManagerException;

    ImportJob replace(LanguagePair direction, Memory memory, String sentence, String translation, String previousSentence, String previousTranslation, Date timestamp, short channel) throws DataManagerException;

    ImportJob replace(LanguagePair direction, Memory memory, String sentence, String translation, String previousSentence, String previousTranslation, Date timestamp, DataChannel channel) throws DataManagerException;

    void delete(long memory) throws DataManagerException;

    DataChannel getDataChannel(short id);

    Map<Short, Long> getChannelsPositions();

    void waitChannelPosition(short channel, long position) throws InterruptedException;

    void waitChannelPositions(Map<Short, Long> positions) throws InterruptedException;

}
