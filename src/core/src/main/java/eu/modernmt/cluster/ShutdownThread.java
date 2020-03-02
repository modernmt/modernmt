package eu.modernmt.cluster;


import eu.modernmt.api.ApiServer;
import eu.modernmt.cluster.services.TranslationServiceProxy;
import eu.modernmt.engine.Engine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

class ShutdownThread extends Thread {

    private final Logger logger = LogManager.getLogger(ClusterNode.class);
    private final ClusterNode node;

    ShutdownThread(ClusterNode node) {
        super("ShutdownThread");
        this.node = node;
    }

    @Override
    public void run() {
        long begin = System.currentTimeMillis();

        // Prevent new API requests
        halt(this.node.api);

        // Close internal services
        halt(this.node.translationService); // wait for all translations to be fulfilled
        halt(this.node.database);
        halt(this.node.binaryLog);

        // Stop all engine components
        halt(this.node.getEngine());

        // Stop external services
        for (EmbeddedService service : this.node.services)
            halt(service);

        // Finally set status and halt hazelcast
        this.node.setStatus(ClusterNode.Status.TERMINATED);
        this.node.hazelcast.shutdown();

        long elapsed = System.currentTimeMillis() - begin;
        logger.info("System halted in " + (elapsed / 1000) + "s");

        // Stop log4j2
        LogManager.shutdown();
    }

    private void halt(TranslationServiceProxy service) {
        if (service == null)
            return;

        try {
            logger.info("Halting Translation Service...");
            service.shutdown();
            service.awaitTermination(1, TimeUnit.DAYS);
            logger.info("Translation Service halted");
        } catch (Throwable e) {
            logger.error("Failed to halt Translation Service", e);
        }
    }

    private void halt(ApiServer api) {
        if (api == null)
            return;

        try {
            logger.info("Halting API interface...");
            api.stop();
            logger.info("API interface halted");
        } catch (Throwable e) {
            logger.error("Failed to halt API interface", e);
        }
    }

    private void halt(Engine engine) {
        if (engine == null)
            return;

        try {
            logger.info("Halting Engine...");
            engine.close();
            logger.info("Engine halted");
        } catch (Throwable e) {
            logger.error("Failed to halt Engine", e);
        }
    }

    private void halt(Closeable closeable) {
        if (closeable == null)
            return;

        String name = closeable.getClass().getSimpleName();
        try {
            logger.info("Halting component \"" + name + "\"...");
            closeable.close();
            logger.info("Component \"" + name + "\" halted");
        } catch (Throwable e) {
            logger.error("Failed to halt \"" + name + "\" component", e);
        }
    }

    private void halt(EmbeddedService service) {
        if (service == null)
            return;

        String name = service.getClass().getSimpleName();
        try {
            logger.info("Halting service \"" + name + "\"...");
            service.shutdown();
            logger.info("Service \"" + name + "\" halted");
        } catch (Throwable e) {
            logger.error("Failed to halt \"" + name + "\" service", e);
        }
    }
}
