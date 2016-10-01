//
// Created by Davide  Caroselli on 19/09/16.
//

#include "BufferedUpdateManager.h"

using namespace mmt::ilm;

BufferedUpdateManager::BufferedUpdateManager(NGramStorage *storage, size_t bufferSize, double maxDelay) :
        storage(storage), waitTimeout(maxDelay), stop(false) {
    foregroundBatch = new NGramBatch(storage->GetOrder(), bufferSize, storage->GetStreamsStatus());
    backgroundBatch = new NGramBatch(storage->GetOrder(), bufferSize, storage->GetStreamsStatus());

    backgroundThread = new boost::thread(boost::bind(&BufferedUpdateManager::BackgroundThreadRun, this));
}

BufferedUpdateManager::~BufferedUpdateManager() {
    stop = true;
    AwakeBackgroundThread();

    backgroundThread->join();

    delete backgroundThread;
    delete foregroundBatch;
    delete backgroundBatch;
}

void BufferedUpdateManager::Add(const updateid_t &id, const domain_t domain, const vector<wid_t> &sentence) {
    bool success = false;

    while (!success) {
        batchAccess.lock();
        success = foregroundBatch->Add(id, domain, sentence);
        batchAccess.unlock();

        if (!success)
            AwakeBackgroundThread();
    }
}

void BufferedUpdateManager::AwakeBackgroundThread() {
    awakeCondition.notify_one();

    unique_lock<mutex> lock(awakeMutex);
    awakeCondition.wait(lock);
}

void BufferedUpdateManager::BackgroundThreadRun() {
    auto timeout = std::chrono::milliseconds((int64_t) (waitTimeout * 1000.));

    while (!stop) {
        unique_lock<mutex> lock(awakeMutex);
        awakeCondition.wait_for(lock, timeout);

        if (!stop) {
            batchAccess.lock();
            {
                NGramBatch *tmp = backgroundBatch;
                backgroundBatch = foregroundBatch;
                foregroundBatch = tmp;

                foregroundBatch->Reset(backgroundBatch->GetStreams());
            }
            batchAccess.unlock();

            if (backgroundBatch->GetSize() > 0) {
                storage->PutBatch(*backgroundBatch);
                backgroundBatch->Clear();
            }
        }

        lock.unlock();
        awakeCondition.notify_one();
    }
}
