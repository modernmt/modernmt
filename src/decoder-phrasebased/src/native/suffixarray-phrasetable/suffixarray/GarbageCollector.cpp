//
// Created by Davide  Caroselli on 14/02/17.
//

#include "GarbageCollector.h"
#include <util/chrono.h>
#include "dbkv.h"

using namespace rocksdb;
using namespace std;
using namespace mmt;
using namespace mmt::sapt;

static const string kPendingDeletionKey = MakeEmptyKey(kPendingDeletionKeyType);

GarbageCollector::GarbageCollector(CorporaStorage *storage, rocksdb::DB *db,
                                   uint8_t prefixLength, size_t batchSize, double timeout)
        : BackgroundPollingThread(timeout), logger("sapt.GarbageCollector"), db(db), storage(storage),
          batchSize(batchSize), prefixLength(prefixLength) {
    // Pending deletion
    string raw_deletion;

    pendingDeletionMemory = 0;
    pendingDeletionOffset = StorageIterator::eof;

    db->Get(ReadOptions(), kPendingDeletionKey, &raw_deletion);
    DeserializePendingDeletionData(raw_deletion.data(), raw_deletion.size(),
                                   &pendingDeletionMemory, &pendingDeletionOffset);

    // Deleted memories
    string deletionKeyPrefix = MakeEmptyKey(kDeletedMemoryKeyType);

    Iterator *it = db->NewIterator(ReadOptions());
    it->Seek(deletionKeyPrefix);

    Slice key;
    while (it->Valid() && (key = it->key()).starts_with(deletionKeyPrefix)) {
        queue.insert(GetMemoryFromDeletionKey(key.data()));
        it->Next();
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

void GarbageCollector::BackgroundThreadRun() throw(index_exception) {
    queueAccess.lock();
    unordered_set<memory_t> memories = queue;
    queueAccess.unlock();

    if (memories.empty())
        return;

    LogInfo(logger) << "Started cleaning process";
    double beginTime = GetTime();

    try {
        if (pendingDeletionMemory != 0) {
            if (pendingDeletionOffset == StorageIterator::eof)
                DeleteStorage(pendingDeletionMemory);
            else
                Delete(pendingDeletionMemory, pendingDeletionOffset);

            pendingDeletionMemory = 0;
            pendingDeletionOffset = StorageIterator::eof;
        }

        for (auto memory = memories.begin(); memory != memories.end(); ++memory) {
            Delete(*memory);
        }

        LogInfo(logger) << "Cleaning process completed in " << GetElapsedTime(beginTime) << "s";
    } catch (interrupted_exception &e) {
        LogInfo(logger) << "Cleaning process interrupted after " << GetElapsedTime(beginTime) << "s";
    }
}

void GarbageCollector::Delete(memory_t memory, int64_t offset) throw(interrupted_exception, index_exception) {
    double beginTime = GetTime();
    LogInfo(logger) << (offset == 0 ? "Deleting memory " : "Resuming deletion of memory ") << memory;

    StorageIterator *iterator = storage->NewIterator(memory, (size_t) offset);
    if (iterator != nullptr) {
        unordered_set<string> prefixKeys;
        unordered_map<string, int64_t> targetCounts;

        int64_t currentOffset;
        do {
            currentOffset = LoadBatch(memory, iterator, &prefixKeys, &targetCounts);

            if (!IsRunning())
                throw interrupted_exception();

            WriteBatch(memory, currentOffset, prefixKeys, targetCounts);
        } while (currentOffset != StorageIterator::eof);

        delete iterator;
    }

    DeleteStorage(memory);

    LogInfo(logger) << "Deletion of memory " << memory << " completed in " << GetElapsedTime(beginTime) << "s";
}

void GarbageCollector::DeleteStorage(memory_t memory) throw(index_exception) {
    storage->Delete(memory);

    rocksdb::WriteBatch writeBatch;
    writeBatch.Delete(kPendingDeletionKey);
    writeBatch.Delete(MakeMemoryDeletionKey(memory));

    Status status = db->Write(WriteOptions(), &writeBatch);
    if (!status.ok())
        throw index_exception("Unable to write to index: " + status.ToString());

    queueAccess.lock();
    queue.erase(memory);
    queueAccess.unlock();
}

int64_t GarbageCollector::LoadBatch(memory_t memory, StorageIterator *iterator,
                                    unordered_set<string> *outPrefixKeys,
                                    unordered_map<string, int64_t> *outTargetCounts) throw(interrupted_exception) {
    outPrefixKeys->clear();
    outTargetCounts->clear();

    vector<wid_t> source;
    vector<wid_t> target;
    alignment_t alignment;

    size_t isRunningCheckCount = 0;

    int64_t offset = 0;
    for (size_t i = 0; i < batchSize; ++i) {
        if ((isRunningCheckCount++ % 1000 == 0) && !IsRunning()) // check every 1000 sentences
            throw interrupted_exception();

        if (!iterator->Next(&source, &target, &alignment, &offset))
            break;

        // Load source prefixes

        size_t sourceSize = source.size();

        for (size_t start = 0; start < sourceSize; ++start) {
            for (size_t length = 1; length <= prefixLength; ++length) {
                if (start + length > sourceSize)
                    break;

                outPrefixKeys->insert(MakePrefixKey(prefixLength, memory, source, start, length));
            }
        }

        // Load target counts

        size_t targetSize = target.size();

        for (size_t start = 0; start < targetSize; ++start) {
            for (size_t length = 1; length <= prefixLength; ++length) {
                if (start + length > targetSize)
                    break;

                string dkey = MakeCountKey(prefixLength, target, start, length);
                (*outTargetCounts)[dkey]++;
            }
        }
    }

    return offset;
}

void GarbageCollector::WriteBatch(memory_t memory, int64_t offset, const unordered_set<string> &prefixKeys,
                                  const unordered_map<string, int64_t> &targetCounts) {
    rocksdb::WriteBatch writeBatch;

    // Add source prefixes to write batch
    for (auto prefix = prefixKeys.begin(); prefix != prefixKeys.end(); ++prefix) {
        writeBatch.Delete(*prefix);
    }

    // Add target counts to write batch
    for (auto count = targetCounts.begin(); count != targetCounts.end(); ++count) {
        string value = SerializeCount(-(count->second));
        writeBatch.Merge(count->first, value);
    }

    // Write deletion state
    writeBatch.Put(kPendingDeletionKey, SerializePendingDeletionData(memory, offset));

    // Commit write batch
    Status status = db->Write(WriteOptions(), &writeBatch);
    if (!status.ok())
        throw index_exception("Unable to write to index: " + status.ToString());
}
