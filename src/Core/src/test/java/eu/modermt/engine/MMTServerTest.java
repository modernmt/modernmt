package eu.modermt.engine;

import eu.modernmt.network.cluster.Cluster;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.network.cluster.Worker;
import eu.modernmt.network.messaging.zeromq.ZMQMessagingClient;
import eu.modernmt.network.messaging.zeromq.ZMQMessagingServer;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by davide on 10/12/15.
 */
public class MMTServerTest {

    // -Djava.library.path=/Users/davide/workspaces/mmt/ModernMT/src/Decoder/target:/usr/local/lib/

//    static {
//        System.setProperty("mmt.engines.path", "/Users/davide/workspaces/mmt/ModernMT/engines");
//    }
//
//    public static void main(String[] args) throws Throwable {
//        TranslationEngine engine = TranslationEngine.get("default");
//        MMTServer server = new MMTServer(engine);
//
//        Thread.sleep(3000);
////
////        System.out.println(server.translate(new Sentence("ciao mondo")));
//
//        Thread.sleep(3000);
//
//        server.shutdown();
//        server.awaitTermination(1, TimeUnit.DAYS);
//    }

    /*









     */

    public static class EWorker extends Worker {

        public EWorker(int port0, int port1) {
            super(new ZMQMessagingClient("localhost", port0, port1), 2);
            this.ready();
        }

        @Override
        protected void onCustomBroadcastSignalReceived(byte signal, byte[] payload, int offset, int length) {
            // Do nothing
        }
    }

    public static class ECluster extends Cluster {

        private static class TestTask extends DistributedCallable<String> {

            private String input;

            public TestTask(String input) {
                this.input = input;
            }

            @Override
            public String call() throws Exception {
                Thread.sleep(300);
                return input + input;
            }
        }

        public ECluster(int port0, int port1) {
            super(new ZMQMessagingServer(port0, port1));
        }

        @Override
        protected byte[] onCustomRequestReceived(byte signal, byte[] payload, int offset, int length) {
            return null;
        }

        public Future<String> test(String input) throws ExecutionException, InterruptedException {
            return this.submit(new TestTask(input));
        }
    }

    public static void main(String[] args) throws Throwable {
        int[] ports = new int[]{5000, 5001};

        Worker worker1 = new EWorker(ports[0], ports[1]);
        worker1.start();

//        Worker worker2 = new EWorker(ports[0], ports[1]);
//        worker2.start();

        ECluster cluster = new ECluster(ports[0], ports[1]);
        cluster.start();

        String example = "Come molti di voi hanno avuto modo recentemente di notare,";
        ArrayList<Future<String>> futures = new ArrayList<>();
        for (String token : example.split("\\s+")) {
            futures.add(cluster.test(token));
        }

        for (Future<String> future : futures)
            System.out.println(future.get());
    }
}
