package eu.modernmt.engine;

import eu.modernmt.network.cluster.Worker;
import eu.modernmt.network.messaging.zeromq.ZMQMessagingClient;
import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 09/12/15.
 */
public class MMTWorker extends Worker {

    private TranslationEngine engine;
    private Initializer initializer;

    public MMTWorker(TranslationEngine engine, int threads) throws IOException {
        this(engine, null, MMTServer.DEFAULT_SERVER_PORTS, threads);
    }

    public MMTWorker(TranslationEngine engine, String host, int[] ports, int threads) throws IOException {
        super(new ZMQMessagingClient(host == null ? "localhost" : host, ports[0], ports[1]), threads);
        this.engine = engine;
        this.initializer = new Initializer();
    }

    public TranslationEngine getEngine() {
        return engine;
    }

    @Override
    public void start() throws IOException {
        super.start();
        this.initializer.start();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.initializer.interrupt();
    }

    @Override
    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        super.awaitTermination(timeout, unit);
        unit.timedJoin(this.initializer, timeout);
    }

    protected void onDecoderWeightsReceived(Map<String, float[]> weights) {
        engine.setDecoderWeights(weights);
        setActive(true);
    }

    @Override
    protected void onCustomBroadcastSignalReceived(byte signal, byte[] payload, int offset, int length) {
        switch (signal) {
            case MMTServer.SIGNAL_RESET:
                setActive(false);
                new Killer().start();
                break;
            default:
                logger.warn("Unknown broadcast signal received: " + Integer.toHexString(signal));
                break;
        }
    }

    private class Initializer extends Thread {

        @Override
        public void run() {
            byte[] response = null;

            while (!isInterrupted() && response == null) {
                try {
                    response = sendRequest(MMTServer.REQUEST_FWEIGHTS, null, TimeUnit.MINUTES, 1);
                } catch (IOException e) {
                    logger.warn("Exception while receiving decoder weights.", e);
                    response = null;
                } catch (InterruptedException e) {
                    response = null;
                }

                if (response != null && response[0] != MMTServer.REQUEST_FWEIGHTS) {
                    logger.warn("Response to REQUEST_FWEIGHTS has wrong type: " + response[0]);
                    response = null;
                }
            }

            if (response != null) {
                Map<String, float[]> weights = null;

                if (response.length > 1) {
                    ByteArrayInputStream stream = new ByteArrayInputStream(response, 1, response.length - 1);
                    weights = SerializationUtils.deserialize(stream);
                }

                onDecoderWeightsReceived(weights);
            }
        }

    }

    private class Killer extends Thread {

        public Killer() {
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                shutdown();
                awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                // Nothing to do
            } finally {
                System.exit(101);
            }
        }
    }

}
