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

    pendingDeletionDomain = 0;
    pendingDeletionOffset = StorageIterator::eof;

    db->Get(ReadOptions(), kPendingDeletionKey, &raw_deletion);
    DeserializePendingDeletionData(raw_deletion.data(), raw_deletion.size(),
                                   &pendingDeletionDomain, &pendingDeletionOffset);

    // Deleted domains
    string deletionKeyPrefix = MakeEmptyKey(kDeletedDomainKeyType);

    Iterator *it = db->NewIterator(ReadOptions());
    it->Seek(deletionKeyPrefix);

    Slice key;
    while (it->Valid() && (key = it->key()).starts_with(deletionKeyPrefix)) {
        queue.insert(GetDomainFromDeletionKey(key.data()));
        it->Next();
    }

    delete it;

    // Starting background thread
    Start();
}

GarbageCollector::~GarbageCollector() {
    Stop();
}

void GarbageCollector::MarkForDeletion(const std::vector<domain_t> &domains) {
    queueAccess.lock();
    queue.insert(domains.begin(), domains.end());
    queueAccess.unlock();
}

void GarbageCollector::BackgroundThreadRun() throw(index_exception) {
    queueAccess.lock();
    unordered_set<domain_t> domains = queue;
    queueAccess.unlock();

    if (domains.empty())
        return;

    LogInfo(logger) << "Started cleaning process";
    double beginTime = GetTime();

    try {
        if (pendingDeletionDomain != 0) {
            if (pendingDeletionOffset == StorageIterator::eof)
                DeleteStorage(pendingDeletionDomain);
            else
                Delete(pendingDeletionDomain, pendingDeletionOffset);

            pendingDeletionDomain = 0;
            pendingDeletionOffset = StorageIterator::eof;
        }

        for (auto domain = domains.begin(); domain != domains.end(); ++domain) {
            Delete(*domain);
        }

        LogInfo(logger) << "Cleaning process completed in " << GetElapsedTime(beginTime) << "s";
    } catch (interrupted_exception &e) {
        LogInfo(logger) << "Cleaning process interrupted after " << GetElapsedTime(beginTime) << "s";
    }
}

void GarbageCollector::Delete(domain_t domain, int64_t offset) throw(interrupted_exception, index_exception) {
    double beginTime = GetTime();
    LogInfo(logger) << (offset == 0 ? "Deleting domain " : "Resuming deletion of domain ") << domain;

    StorageIterator *iterator = storage->NewIterator(domain, (size_t) offset);
    if (iterator != nullptr) {
        unordered_set<string> prefixKeys;
        unordered_map<string, int64_t> targetCounts;

        int64_t currentOffset;
        do {
            currentOffset = LoadBatch(domain, iterator, &prefixKeys, &targetCounts);

            if (!IsRunning())
                throw interrupted_exception();

            WriteBatch(domain, currentOffset, prefixKeys, targetCounts);
        } while (currentOffset != StorageIterator::eof);

        delete iterator;
    }

    DeleteStorage(domain);

    LogInfo(logger) << "Deletion of domain " << domain << " completed in " << GetElapsedTime(beginTime) << "s";
}

void GarbageCollector::DeleteStorage(domain_t domain) throw(index_exception) {
    storage->Delete(domain);

    rocksdb::WriteBatch writeBatch;
    writeBatch.Delete(kPendingDeletionKey);
    writeBatch.Delete(MakeDomainDeletionKey(domain));

    Status status = db->Write(WriteOptions(), &writeBatch);
    if (!status.ok())
        throw index_exception("Unable to write to index: " + status.ToString());

    queueAccess.lock();
    queue.erase(domain);
    queueAccess.unlock();
}

int64_t GarbageCollector::LoadBatch(domain_t domain, StorageIterator *iterator,
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

                outPrefixKeys->insert(MakePrefixKey(prefixLength, domain, source, start, length));
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

void GarbageCollector::WriteBatch(domain_t domain, int64_t offset, const unordered_set<string> &prefixKeys,
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
    writeBatch.Put(kPendingDeletionKey, SerializePendingDeletionData(domain, offset));

    // Commit write batch
    Status status = db->Write(WriteOptions(), &writeBatch);
    if (!status.ok())
        throw index_exception("Unable to write to index: " + status.ToString());
}
