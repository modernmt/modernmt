package eu.modernmt.util;

/**
 * Created by davide on 14/12/16.
 */
public class Timer {

    private long epoch = System.currentTimeMillis();

    public void reset() {
        epoch = System.currentTimeMillis();
    }

    public long time() {
        return System.currentTimeMillis() - epoch;
    }

}
