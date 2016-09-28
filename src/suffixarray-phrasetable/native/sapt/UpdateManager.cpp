//
// Created by Davide  Caroselli on 27/09/16.
//

#include "UpdateManager.h"

using namespace mmt::sapt;

UpdateManager::UpdateManager(SuffixArray *index, size_t bufferSize, double maxDelay) :
        index(index), waitTimeout(maxDelay), stop(false) {
    foregroundBatch = new UpdateBatch(bufferSize, index->GetStreams());
    backgroundBatch = new UpdateBatch(bufferSize, index->GetStreams());

    backgroundThread = new boost::thread(boost::bind(&UpdateManager::BackgroundThreadRun, this));
}

UpdateManager::~UpdateManager() {
    stop = true;
    AwakeBackgroundThread(false);

    backgroundThread->join();

    delete backgroundThread;
    delete foregroundBatch;
    delete backgroundBatch;
}

void UpdateManager::Add(const updateid_t &id, const domain_t domain, const vector<wid_t> &source,
                        const vector<wid_t> &target, const alignment_t &alignment) {
    bool success = false;

    while (!success) {
        batchAccess.lock();
        success = foregroundBatch->Add(id, domain, source, target, alignment);
        batchAccess.unlock();

        if (!success)
            AwakeBackgroundThread(true);
    }
}

void UpdateManager::AwakeBackgroundThread(bool wait) {
    awakeCondition.notify_one();

    if (wait) {
        unique_lock<mutex> lock(awakeMutex);
        awakeCondition.wait(lock);
    }
}

void UpdateManager::BackgroundThreadRun() {
    auto timeout = std::chrono::milliseconds((int64_t) (waitTimeout * 1000.));

    while (!stop) {
        unique_lock<mutex> lock(awakeMutex);
        awakeCondition.wait_for(lock, timeout);

        if (!stop) {
            batchAccess.lock();
            {
                UpdateBatch *tmp = backgroundBatch;
                backgroundBatch = foregroundBatch;
                foregroundBatch = tmp;

                foregroundBatch->Reset(backgroundBatch->GetStreams());
            }
            batchAccess.unlock();

            if (backgroundBatch->GetSize() > 0) {
                index->PutBatch(*backgroundBatch);
                backgroundBatch->Clear();
            }
        }

        lock.unlock();
        awakeCondition.notify_one();
    }
}
