package eu.modernmt.data;

import java.util.Collection;

/**
 * Created by davide on 06/09/16.
 */
public interface DataListenerProvider {

    Collection<LogDataListener> getDataListeners();

}
