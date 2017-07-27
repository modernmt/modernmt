package eu.modernmt.data;

import eu.modernmt.model.ImportJob;
import eu.modernmt.model.LanguagePair;
import eu.modernmt.model.corpus.BilingualCorpus;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 06/09/16.
 */
public interface DataManager extends Closeable {

    short DOMAIN_UPLOAD_CHANNEL_ID = 0;
    short CONTRIBUTIONS_CHANNEL_ID = 1;

    interface Listener {
        void onDataBatchProcessed(Map<Short, Long> updatedPositions);
    }

    Map<Short, Long> connect(String host, int port, long timeout, TimeUnit unit) throws HostUnreachableException;

    void setDataManagerListener(Listener listener);

    void addDataListener(DataListener listener);

    ImportJob upload(long domainId, BilingualCorpus corpus, short channel) throws DataManagerException;

    ImportJob upload(long domainId, BilingualCorpus corpus, DataChannel channel) throws DataManagerException;

    ImportJob upload(LanguagePair direction, long domainId, String sourceSentence, String targetSentence, short channel) throws DataManagerException;

    ImportJob upload(LanguagePair direction, long domainId, String sourceSentence, String targetSentence, DataChannel channel) throws DataManagerException;

    void delete(long domainId) throws DataManagerException;

    DataChannel getDataChannel(short id);

    Map<Short, Long> getChannelsPositions();

    void waitChannelPosition(short channel, long position) throws InterruptedException;

    void waitChannelPositions(Map<Short, Long> positions) throws InterruptedException;

}
