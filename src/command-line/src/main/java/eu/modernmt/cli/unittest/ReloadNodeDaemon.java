package eu.modernmt.cli.unittest;

import eu.modernmt.cli.ClusterNodeMain;
import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.error.FailedToJoinClusterException;
import eu.modernmt.config.ConfigException;
import eu.modernmt.config.NodeConfig;
import eu.modernmt.config.xml.XMLConfigBuilder;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.Engine;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.io.RuntimeIOException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class ReloadNodeDaemon extends Thread {

    private final Logger logger = LogManager.getLogger(ReloadNodeDaemon.class);

    private final SynchronousQueue<Object> poisonPill = new SynchronousQueue<>();

    private final File sentinel;
    private final NodeConfig config;
    private final ClusterNodeMain.FileStatusListener listener;

    public ReloadNodeDaemon(File sentinel, NodeConfig config, ClusterNodeMain.FileStatusListener listener) {
        this.sentinel = sentinel;
        this.config = config;
        this.listener = listener;
    }

    @Override
    public synchronized void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Reload Daemon");
            try {
                ReloadNodeDaemon.this.poisonPill.put("die");
            } catch (InterruptedException e) {
                ReloadNodeDaemon.this.interrupt();
            }
        }));

        super.start();
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Object pill = poisonPill.poll(500, TimeUnit.MILLISECONDS);
                if (pill != null)
                    break;

                if (sentinel.isFile())
                    reload();
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void reload() throws InterruptedException {
        try {
            String name = FileUtils.readFileToString(sentinel).trim();
            logger.info("Received reload request for engine: " + name);

            NodeConfig config = XMLConfigBuilder.build(Engine.getConfigFile(name));
            config.getEngineConfig().setName(name);

            ClusterNode node = ModernMT.getNode();
            if (node != null) {
                node.shutdown();
                node.awaitTermination(1, TimeUnit.HOURS);
            }

            Log4jConfiguration.setup(1);
            ModernMT.start(config, listener);
            listener.updateStatus(ClusterNode.Status.RUNNING).store();

            FileUtils.forceDelete(sentinel);
        } catch (BootstrapException | ConfigException e) {
            throw new RuntimeException("Failed to reload engine: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        } catch (FailedToJoinClusterException e) {
            throw new RuntimeException("Failed to join cluster", e);
        }
    }
}
