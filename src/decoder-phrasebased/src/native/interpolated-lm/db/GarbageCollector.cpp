//
// Created by Davide  Caroselli on 17/02/17.
//

#include <util/chrono.h>
#include "GarbageCollector.h"
#include "dbkv.h"

using namespace rocksdb;
using namespace std;
using namespace mmt;
using namespace mmt::ilm;

GarbageCollector::GarbageCollector(rocksdb::DB *db, double timeout) : BackgroundPollingThread(timeout), db(db) {
    // Deleted memories
    Iterator *it = db->NewIterator(ReadOptions());

    for (it->SeekToFirst(); it->Valid(); it->Next()) {
        Slice key = it->key();
        if (!HasMemoryDeletionPrefix(key.data(), key.size()))
            break;

        memory_t memory = GetMemoryFromDeletionKey(key.data(), key.size());
        if (memory > 0)
            queue.insert(memory);
    }

    delete it;

    // Starting background thread
    Start();
}

GarbageCollector::~GarbageCollector() {
    Stop();
}

void GarbageCollector::MarkForDeletion(const std::vector<memory_t> &memories) {
    queueAccess.lock();
    queue.insert(memories.begin(), memories.end());
    queueAccess.unlock();
}

void GarbageCollector::BackgroundThreadRun() {
    queueAccess.lock();
    unordered_set<memory_t> memories = queue;
    queueAccess.unlock();

    if (memories.empty())
        return;

    LogInfo(logger) << "Started cleaning process";
    double beginTime = GetTime();

    try {
        for (auto memory = memories.begin(); memory != memories.end(); ++memory) {
            Delete(*memory);

            db->Delete(WriteOptions(), MakeMemoryDeletionKey(*memory));

            queueAccess.lock();
            queue.erase(*memory);
            queueAccess.unlock();
        }

        LogInfo(logger) << "Cleaning process completed in " << GetElapsedTime(beginTime) << "s";
    } catch (interrupted_exception &e) {
        LogInfo(logger) << "Cleaning process interrupted after " << GetElapsedTime(beginTime) << "s";
    }
}

void GarbageCollector::Delete(memory_t memory) throw(interrupted_exception) {
    if (!IsRunning())
        throw interrupted_exception();

    double beginTime = GetTime();
    LogInfo(logger) << "Deleting memory " << memory;

    WriteOptions writeOptions;

    string entryKey = MakeNGramKey(memory, 0);
    Iterator *it = db->NewIterator(ReadOptions());

    for (it->Seek(entryKey); it->Valid(); it->Next()) {
        Slice key = it->key();

        memory_t keyMemory;
        ngram_hash_t keyHash;
        GetNGramKeyData(key.data(), key.size(), &keyMemory, &keyHash);

        if (memory != keyMemory)
            break;

        db->Delete(writeOptions, key);
    }

    delete it;

    LogInfo(logger) << "Deletion of memory " << memory << " completed in " << GetElapsedTime(beginTime) << "s";
}
