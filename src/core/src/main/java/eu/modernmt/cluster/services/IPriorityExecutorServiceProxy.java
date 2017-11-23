package eu.modernmt.cluster.services;

import com.hazelcast.core.Member;
import com.hazelcast.executor.impl.DistributedExecutorService;
import com.hazelcast.instance.MemberImpl;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.AbstractDistributedObject;
import com.hazelcast.spi.InternalCompletableFuture;
import com.hazelcast.spi.NodeEngine;

import java.util.concurrent.Callable;

import static com.hazelcast.util.Preconditions.checkNotNull;
import static com.hazelcast.util.UuidUtil.newUnsecureUuidString;

public class IPriorityExecutorServiceProxy extends AbstractDistributedObject<DistributedPriorityExecutorService> implements IPriorityExecutorService {
    private final String name;
    private final DistributedPriorityExecutorService service;

    public IPriorityExecutorServiceProxy(NodeEngine nodeEngine, String name, DistributedPriorityExecutorService service) {
        super(nodeEngine, service);
        this.name = name;
        this.service = service;
    }

    @Override
    public String getPartitionKey() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getServiceName() {
        return DistributedPriorityExecutorService.SERVICE_NAME;
    }

    public <T> InternalCompletableFuture<T> submitToMember(Callable<T> callable, Priority priority, Member member) {
        checkNotNull(callable, "task can't be null");
        //checkNotShutdown();

        NodeEngine nodeEngine = getNodeEngine();
        String uuid = newUnsecureUuidString();
        Address target = ((MemberImpl) member).getAddress();

        //boolean sync = checkSync();
        IPriorityCallableOperation operation = new IPriorityCallableOperation<T>(callable, priority);

        return nodeEngine.getOperationService()
                .invokeOnTarget(DistributedExecutorService.SERVICE_NAME, operation, target);
    }
}
