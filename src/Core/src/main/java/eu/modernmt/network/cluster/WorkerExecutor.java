package eu.modernmt.network.cluster;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 11/12/15.
 */
class WorkerExecutor {

    private final Worker worker;
    private final int capacity;
    private int availability;

    private ExecutorService executorService;

    public WorkerExecutor(Worker worker, int capacity) {
        this.worker = worker;
        this.capacity = capacity;
        this.availability = capacity;
        this.executorService = Executors.newFixedThreadPool(capacity);
    }

    public int getAvailability() {
        return availability;
    }

    public int getCapacity() {
        return capacity;
    }

    public void submit(CallableRequest[] requests) {
        for (CallableRequest request : requests) {
            executorService.submit(new Task(request));
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    public void awaitTermination() throws InterruptedException {
        executorService.awaitTermination(365, TimeUnit.DAYS);
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        executorService.awaitTermination(timeout, unit);
    }

    private class Task implements Runnable {

        private CallableRequest request;

        public Task(CallableRequest request) {
            this.request = request;
        }

        @Override
        public void run() {
            availability--;

            try {
                request.callable.setWorker(worker);

                CallableResponse response;
                try {
                    response = CallableResponse.fromResult(request.id, request.callable.call());
                } catch (Exception e) {
                    response = CallableResponse.fromError(request.id, e);
                }

                request.callable.setWorker(null);
                worker.sendResponse(response);
            } finally {
                availability++;
            }
        }

    }

}
