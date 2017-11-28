
package eu.modernmt.cluster.services;

import com.hazelcast.nio.Address;
import com.hazelcast.spi.AbstractDistributedObject;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.OperationService;
import eu.modernmt.cluster.TranslationTask;
import eu.modernmt.model.Translation;

import java.util.concurrent.Future;

/**
 * A TranslationServiceProxy is an Hazelcast proxy for a TranslationService service.
 * @see TranslationService
 *
 * It is used by cluster members as a local endpoint to a remote TranslationService instance.
 *
 * A TranslationServiceProxy is typically spawned by a TranslationService instance in its own cluster node,
 * and keeps a reference to its TranslationService.
 */
public class TranslationServiceProxy extends AbstractDistributedObject<TranslationService> {

    private final String name;

    protected TranslationServiceProxy(NodeEngine nodeEngine, TranslationService service, String name) {
        super(nodeEngine, service);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getServiceName() {
        return TranslationService.SERVICE_NAME;
    }

    /**
     * This method allows this cluster Member to ask another Member to run a TranslationTask.
     * The local TranslationServiceProxy creates a TranslationOperation for the task and
     * uses the local OperationService to pass it to the TranslationService of the remote member.
     *
     * @param task the TranslationTask to run
     * @param address the Address of the Member that should run this task
     * @return a Future for the Translation that this task will output
     */
    public Future<Translation> submit(TranslationTask task, Address address) {
        OperationService localOperationService = getNodeEngine().getOperationService();
        TranslationOperation operation = new TranslationOperation(task);
        return localOperationService.invokeOnTarget(getServiceName(), operation, address);
    }
}