//
// Created by Davide  Caroselli on 19/09/16.
//

#include "BufferedUpdateManager.h"

using namespace mmt::ilm;

BufferedUpdateManager::BufferedUpdateManager(NGramStorage *storage, size_t bufferSize, double maxDelay) :
        BackgroundPollingThread(maxDelay), storage(storage) {
    foregroundBatch = new NGramBatch(storage->GetOrder(), bufferSize, storage->GetStreamsStatus());
    backgroundBatch = new NGramBatch(storage->GetOrder(), bufferSize, storage->GetStreamsStatus());

    Start();
}

BufferedUpdateManager::~BufferedUpdateManager() {
    Stop();

    delete foregroundBatch;
    delete backgroundBatch;
}

#define UpdateManagerEnqueue(_line) \
    bool success = false; \
\
    while (!success) { \
        batchAccess.lock(); \
        success = _line; \
        batchAccess.unlock(); \
\
        if (!success) \
            AwakeBackgroundThread(true); \
    }

void BufferedUpdateManager::Add(const updateid_t &id, const domain_t domain, const vector<wid_t> &sentence) {
    UpdateManagerEnqueue(
            foregroundBatch->Add(id, domain, sentence);
    );
}

void BufferedUpdateManager::Delete(const mmt::updateid_t &id, const mmt::domain_t domain) {
    UpdateManagerEnqueue(
            foregroundBatch->Delete(id, domain)
    );
}

void BufferedUpdateManager::BackgroundThreadRun() {
    batchAccess.lock();
    {
        NGramBatch *tmp = backgroundBatch;
        backgroundBatch = foregroundBatch;
        foregroundBatch = tmp;

        foregroundBatch->Reset(backgroundBatch->GetStreams());
    }
    batchAccess.unlock();

    if (!backgroundBatch->IsEmpty()) {
        storage->PutBatch(*backgroundBatch);
        backgroundBatch->Clear();
    }
}
