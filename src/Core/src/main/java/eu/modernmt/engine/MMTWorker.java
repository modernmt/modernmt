//package eu.modernmt.engine;
//
//import eu.modernmt.network.cluster.Worker;
//import eu.modernmt.network.messaging.zeromq.ZMQMessagingClient;
//
//import java.io.IOException;
//import java.util.concurrent.TimeUnit;
//
///**
// * Created by davide on 09/12/15.
// */
//public class MMTWorker extends Worker<ServerStatus> {
//
//    private TranslationEngine engine;
//
//    public MMTWorker(TranslationEngine engine) throws IOException {
//        this(engine, "localhost", MMTServer.DEFAULT_SERVER_PORTS);
//    }
//
//    public MMTWorker(TranslationEngine engine, String host, int[] ports) throws IOException {
//        super(new ZMQMessagingClient(host, ports));
//        this.engine = engine;
//    }
//
//    public TranslationEngine getEngine() {
//        return engine;
//    }
//
//    @Override
//    protected void setInitialStatus(ServerStatus status) {
//        engine.setDecoderWeights(status.decoderWeights);
//    }
//
//    @Override
//    protected void updateStatus(ServerStatus status) {
//        engine.setDecoderWeights(status.decoderWeights);
//
//        try {
//            this.shutdown();
//            this.awaitTermination(1, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            // Nothing to do
//        } finally {
//            System.exit(101);
//        }
//    }
//
//}
