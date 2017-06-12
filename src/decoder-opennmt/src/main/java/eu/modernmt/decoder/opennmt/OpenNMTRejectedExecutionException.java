package eu.modernmt.decoder.opennmt;

/**
 * Created by davide on 22/05/17.
 */
public class OpenNMTRejectedExecutionException extends OpenNMTException {

    public OpenNMTRejectedExecutionException() {
        super("Execution rejected: decoder has been closed");
    }

}
