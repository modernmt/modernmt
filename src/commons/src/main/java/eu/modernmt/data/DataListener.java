package eu.modernmt.data;

import java.util.List;
import java.util.Map;

/**
 * Created by davide on 06/09/16.
 */
public interface DataListener {

    void onDataReceived(List<TranslationUnit> batch) throws Exception;

    void onDelete(Deletion deletion) throws Exception;

    Map<Short, Long> getLatestChannelPositions();

}
