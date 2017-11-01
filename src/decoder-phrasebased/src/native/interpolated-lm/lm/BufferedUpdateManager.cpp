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

void BufferedUpdateManager::Add(const mmt::update_batch_t &batch) {
    batchAccess.lock();

    for (auto it = batch.translation_units.begin(); it != batch.translation_units.end(); ++it)
        foregroundBatch->Add(it->channel, it->position, it->memory, it->source);

    for (auto it = batch.deletions.begin(); it != batch.deletions.end(); ++it)
        foregroundBatch->Delete(it->channel, it->position, it->memory);

    foregroundBatch->Advance(batch.channelPositions);

    bool full = foregroundBatch->IsFull();

    batchAccess.unlock();

    if (full)
        AwakeBackgroundThread(true);
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
