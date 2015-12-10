package eu.modernmt.engine;

import eu.modernmt.network.cluster.Worker;
import eu.modernmt.network.messaging.zeromq.ZMQMessagingClient;

import java.io.IOException;

/**
 * Created by davide on 09/12/15.
 */
public class MMTWorker extends Worker {

    private TranslationEngine engine;

    public MMTWorker(TranslationEngine engine) throws IOException {
        this(engine, "localhost", MMTServer.DEFAULT_SERVER_PORT);
    }

    public MMTWorker(TranslationEngine engine, String host, int port) throws IOException {
        super(new ZMQMessagingClient(host, port));
        this.engine = engine;
    }

    public TranslationEngine getEngine() {
        return engine;
    }
}
