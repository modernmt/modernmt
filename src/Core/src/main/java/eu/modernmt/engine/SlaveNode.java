package eu.modernmt.engine;

import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.moses.MosesDecoder;
import eu.modernmt.decoder.moses.MosesINI;
import eu.modernmt.network.cluster.Worker;
import eu.modernmt.network.messaging.zeromq.ZMQMessagingClient;
import eu.modernmt.tokenizer.DetokenizerPool;
import eu.modernmt.tokenizer.TokenizerPool;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 09/12/15.
 */
public class SlaveNode extends Worker {

    public static class MasterHost {

        public String host = null;
        public String user = null;
        public String password = null;
        public File pem = null;

    }

    private static final int DEFAULT_DECODER_THREADS;
    private static final int DEFAULT_SA_WORKERS;

    static {
        int cores = Runtime.getRuntime().availableProcessors();
        DEFAULT_DECODER_THREADS = cores;
        DEFAULT_SA_WORKERS = cores > 3 ? 2 : 1;
    }

    private TranslationEngine engine;
    private Initializer initializer;
    private Throwable initializationError;
    private Decoder decoder;
    private File runtimePath;
    private MasterHost master;

    public SlaveNode(TranslationEngine engine, MasterHost master, int[] ports) throws IOException {
        super(new ZMQMessagingClient(master == null ? "localhost" : master.host, ports[0], ports[1]), DEFAULT_DECODER_THREADS);

        this.engine = engine;
        this.master = master;
        this.initializer = new Initializer();
        this.runtimePath = FS.getRuntime(engine, "slave");
    }

    public TranslationEngine getEngine() {
        return engine;
    }

    public Decoder getDecoder() {
        if (decoder == null)
            throw new IllegalStateException("Decoder has not been initialized yet");

        return decoder;
    }

    private void loadDecoder() throws IOException {
        if (decoder == null) {
            synchronized (this) {
                if (decoder == null) {
                    Map<String, float[]> weights = engine.getDecoderWeights();
                    MosesINI mosesINI = MosesINI.load(engine.getDecoderINITemplate(), engine.getPath());

                    if (weights != null)
                        mosesINI.setWeights(weights);

                    mosesINI.setThreads(DEFAULT_DECODER_THREADS);
                    mosesINI.setWorkers(DEFAULT_SA_WORKERS);


                    File inifile = new File(runtimePath, "moses.ini");
                    FileUtils.write(inifile, mosesINI.toString(), false);
                    decoder = new MosesDecoder(inifile);
                }
            }
        }
    }

    public TokenizerPool getTokenizer() {
        return TokenizerPool.getCachedInstance(engine.getSourceLanguage());
    }

    public DetokenizerPool getDetokenizer() {
        return DetokenizerPool.getCachedInstance(engine.getTargetLanguage());
    }

    @Override
    public void start() throws IOException {
        super.start();
        this.initializer.start();
        logger.info("MMT Cluster Worker startup.");
    }

    public void awaitInitialization() throws Throwable {
        if (isActive())
            return;

        if (initializationError != null)
            throw initializationError;

        this.initializer.join();

        if (initializationError != null)
            throw initializationError;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.initializer.interrupt();
        logger.info("MMT Cluster Worker shutdown.");
    }

    @Override
    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        super.awaitTermination(timeout, unit);
        unit.timedJoin(this.initializer, timeout);
    }

    protected void onSyncPathReceived(String remotePath) throws IOException {
        logger.info("Synchronizing models with " + (master == null ? "localhost" : master.host));
        EngineSynchronizer synchronizer = new EngineSynchronizer(master, engine.getPath(), remotePath);
        synchronizer.sync();

        this.loadDecoder();
        this.setActive(true);

        logger.info("Synchronization complete");
    }

    @Override
    protected void onCustomBroadcastSignalReceived(byte signal, byte[] payload, int offset, int length) {
        switch (signal) {
            case MasterNode.SIGNAL_RESET:
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
            try {
                byte[] response = null;

                while (!isInterrupted() && response == null) {
                    try {
                        response = sendRequest(MasterNode.REQUEST_SYNC_PATH, null, TimeUnit.MINUTES, 1);
                    } catch (IOException e) {
                        logger.warn("Exception while receiving sync path.", e);
                        response = null;
                    } catch (InterruptedException e) {
                        response = null;
                    }

                    if (response != null && response[0] != MasterNode.REQUEST_SYNC_PATH) {
                        logger.warn("Response to REQUEST_SYNC_PATH has wrong type: " + response[0]);
                        response = null;
                    }
                }

                if (response != null) {
                    String remotePath = new String(response, 1, response.length - 1, "UTF-8");
                    onSyncPathReceived(remotePath);
                }
            } catch (Throwable e) {
                logger.error("Error while syncronizing with " + (master == null ? "localhost" : master.host), e);
                initializationError = e;

                shutdown();
            }
        }

    }

    private class Killer extends Thread {

        @Override
        public void run() {
            try {
                shutdown();
                awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Nothing to do
            } finally {
                System.exit(101);
            }
        }
    }

}
