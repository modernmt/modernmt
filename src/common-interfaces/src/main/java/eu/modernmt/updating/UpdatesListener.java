package eu.modernmt.updating;

import java.util.Map;

/**
 * Created by davide on 06/09/16.
 */
public interface UpdatesListener {

    void updateReceived(Update update) throws Exception;

    Map<Integer, Long> getLatestSequentialNumbers();

}
