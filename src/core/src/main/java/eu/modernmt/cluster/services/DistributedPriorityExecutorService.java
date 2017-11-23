package eu.modernmt.cluster.services;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.spi.ManagedService;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.RemoteService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class DistributedPriorityExecutorService implements ManagedService, RemoteService {
    /* this is the equivalent of the Hazelcast DistributedExecutorService for tasks with priority.
    The Hazelcast DistributedExecutorService is the Service object for IExecutorService
    (NB: the IExecutorService despite its name is not a Hazelcast Service, but it is an interface for the service proxy)
     So it is a Service class implementing ManagedService and RemoteService. */

    private final Logger logger = LogManager.getLogger(DistributedPriorityExecutorService.class);
    public static final String SERVICE_NAME = "priority-executor-service";
    private NodeEngine nodeEngine;


    @Override
    public void init(NodeEngine nodeEngine, Properties properties) {
        /* this service will be instantiated (and its "init" method called) by Hazelcast automatically during start.
           I do hope that under the hood all cluster nodes have a service instance, so that when a proxy says
           "execute this operation on target member", it is the target member address to run the operation... */
        this.nodeEngine = nodeEngine;
        logger.info("Priority Executor Service initialized");
    }

    @Override
    public void reset() {
        // Nothing to do
        logger.info("Priority Executor Service reset");
    }

    @Override
    public void shutdown(boolean terminate) {
        // Nothing to do
        logger.info("Priority Executor Service shutdown");
    }

    @Override
    public DistributedObject createDistributedObject(String objectName) {
        return new IPriorityExecutorServiceProxy(this.nodeEngine, objectName, this);
    }

    @Override
    public void destroyDistributedObject(String objectName) {
        logger.info("Priority Executor Service shutdown");
    }
}
