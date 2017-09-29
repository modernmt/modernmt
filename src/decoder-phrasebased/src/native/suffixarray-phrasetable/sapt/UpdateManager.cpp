//
// Created by Davide  Caroselli on 27/09/16.
//

#include "UpdateManager.h"

using namespace mmt::sapt;

UpdateManager::UpdateManager(SuffixArray *index, size_t bufferSize, double maxDelay) :
        BackgroundPollingThread(maxDelay), index(index) {
    foregroundBatch = new UpdateBatch(bufferSize, index->GetStreams());
    backgroundBatch = new UpdateBatch(bufferSize, index->GetStreams());

    Start();
}

UpdateManager::~UpdateManager() {
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

void UpdateManager::Add(const updateid_t &id, const memory_t memory, const vector<wid_t> &source,
                        const vector<wid_t> &target, const alignment_t &alignment) {
    UpdateManagerEnqueue(
            foregroundBatch->Add(id, memory, source, target, alignment)
    );
}

void UpdateManager::Delete(const mmt::updateid_t &id, const mmt::memory_t memory) {
    UpdateManagerEnqueue(
            foregroundBatch->Delete(id, memory)
    );
}

void UpdateManager::BackgroundThreadRun() {
    batchAccess.lock();
    {
        UpdateBatch *tmp = backgroundBatch;
        backgroundBatch = foregroundBatch;
        foregroundBatch = tmp;

        foregroundBatch->Reset(backgroundBatch->GetStreams());
    }
    batchAccess.unlock();

    if (!backgroundBatch->IsEmpty()) {
        index->PutBatch(*backgroundBatch);
        backgroundBatch->Clear();
    }
}