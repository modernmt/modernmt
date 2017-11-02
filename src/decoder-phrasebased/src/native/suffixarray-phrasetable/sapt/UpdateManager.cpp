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

void UpdateManager::Add(const mmt::update_batch_t &batch) {
    batchAccess.lock();

    for (auto it = batch.translation_units.begin(); it != batch.translation_units.end(); ++it)
        foregroundBatch->Add(it->channel, it->position, it->memory, it->source, it->target, it->alignment);

    for (auto it = batch.deletions.begin(); it != batch.deletions.end(); ++it)
        foregroundBatch->Delete(it->channel, it->position, it->memory);

    foregroundBatch->Advance(batch.channelPositions);

    bool full = foregroundBatch->IsFull();

    batchAccess.unlock();

    if (full)
        AwakeBackgroundThread(true);
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