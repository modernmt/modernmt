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

GarbageCollector::GarbageCollector(CorporaStorage *storage, rocksdb::DB *db, uint8_t prefixLength,
                                   const std::unordered_set<domain_t> &domains, size_t batchSize, double timeout)
        : BackgroundPollingThread(timeout), logger("sapt.GarbageCollector"), db(db), storage(storage),
          batchSize(batchSize), prefixLength(prefixLength), queue(domains) {
    // Pending deletion
    string raw_data;

    db->Get(ReadOptions(), kPendingDeletionKey, &raw_data);
    if (!DeserializeDeletionData(raw_data.data(), raw_data.size(), &pendingDeletionDomain, &pendingDeletionOffset)) {
        pendingDeletionDomain = 0;
        pendingDeletionOffset = 0;
    }

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

std::unordered_set<domain_t> GarbageCollector::GetDomainsMarkedForDeletion() {
    unordered_set<domain_t> result;

    queueAccess.lock();
    result = queue;
    queueAccess.unlock();

    return result;
}

void GarbageCollector::BackgroundThreadRun() throw(index_exception) {
    unordered_set<domain_t> domains = GetDomainsMarkedForDeletion();

    if (domains.empty() && pendingDeletionDomain == 0)
        return;

    LogInfo(logger) << "Started cleaning process";
    double beginTime = GetTime();

    try {
        if (pendingDeletionDomain != 0) {
            if (pendingDeletionOffset == 0)
                DeleteStorage(pendingDeletionDomain);
            else
                Delete(pendingDeletionDomain, pendingDeletionOffset);

            pendingDeletionDomain = 0;
            pendingDeletionOffset = 0;
        }

        for (auto domain = domains.begin(); domain != domains.end(); ++domain) {
            Delete(*domain);
        }

        LogInfo(logger) << "Cleaning process completed in " << GetElapsedTime(beginTime) << "s";
    } catch (interrupted_exception &e) {
        LogInfo(logger) << "Cleaning process interrupted after " << GetElapsedTime(beginTime) << "s";
    }
}

void GarbageCollector::Delete(domain_t domain, size_t offset) throw(interrupted_exception, index_exception) {
    double beginTime = GetTime();
    LogInfo(logger) << (offset == 0 ? "Deleting domain " : "Resuming deletion of domain ") << domain;

    StorageIterator *iterator = storage->NewIterator(domain, offset);
    if (iterator != nullptr) {
        unordered_set<string> prefixKeys;
        unordered_map<string, int64_t> targetCounts;

        size_t currentOffset;
        do {
            currentOffset = LoadBatch(domain, iterator, &prefixKeys, &targetCounts);

            if (!IsRunning())
                throw interrupted_exception();

            WriteBatch(domain, currentOffset, prefixKeys, targetCounts);
        } while (currentOffset != StorageIterator::eof);

        delete iterator;
    }

    DeleteStorage(domain);

    queueAccess.lock();
    queue.erase(domain);
    queueAccess.unlock();

    LogInfo(logger) << "Deletion of domain " << domain << " completed in " << GetElapsedTime(beginTime) << "s";
}

void GarbageCollector::DeleteStorage(domain_t domain) {
    storage->Delete(domain);
    db->Delete(WriteOptions(), kPendingDeletionKey);
}

size_t GarbageCollector::LoadBatch(domain_t domain, StorageIterator *iterator,
                                   unordered_set<string> *outPrefixKeys,
                                   unordered_map<string, int64_t> *outTargetCounts) throw(interrupted_exception) {
    outPrefixKeys->clear();
    outTargetCounts->clear();

    vector<wid_t> source;
    vector<wid_t> target;
    alignment_t alignment;

    size_t isRunningCheckCount = 0;

    size_t offset = 0;
    for (size_t i = 0; i < batchSize; ++i) {
        if ((isRunningCheckCount++ % 1000 == 0) && !IsRunning()) // check every 1000 sentences
            throw interrupted_exception();

        if ((offset = iterator->Next(&source, &target, &alignment)) == StorageIterator::eof)
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

void GarbageCollector::WriteBatch(domain_t domain, size_t offset, const unordered_set<string> &prefixKeys,
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
    writeBatch.Put(kPendingDeletionKey, SerializeDeletionData(domain, offset));

    // Commit write batch
    Status status = db->Write(WriteOptions(), &writeBatch);
    if (!status.ok())
        throw index_exception("Unable to write to index: " + status.ToString());
}
