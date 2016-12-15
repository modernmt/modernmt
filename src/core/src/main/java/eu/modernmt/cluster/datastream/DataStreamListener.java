package eu.modernmt.cluster.datastream;

/**
 * Created by davide on 15/12/16.
 */
public interface DataStreamListener {

    void onUpdatesReveiced(long newOffset);

}
