package eu.modernmt.data;

import java.util.Map;

/**
 * Created by davide on 06/09/16.
 */
public interface LogDataListener {

    void onDataReceived(DataBatch batch) throws Exception;

    Map<Short, Long> getLatestChannelPositions();

    boolean needsProcessing();

    boolean needsAlignment();

}
