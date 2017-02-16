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

static const string kPendingDeletionDataKey = MakeEmptyKey(kPendingDeletionKeyType);

GarbageCollector::GarbageCollector(CorporaStorage *storage, rocksdb::DB *db,
                                   const std::unordered_set<domain_t> &domains, size_t batchSize, double timeout)
        : BackgroundPollingThread(timeout), logger("sapt.GarbageCollector"), db(db), storage(storage),
          batchSize(batchSize), queue(domains) {
    // Pending deletion
    string raw_data;

    db->Get(ReadOptions(), kPendingDeletionDataKey, &raw_data);
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

void GarbageCollector::BackgroundThreadRun() {
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

void GarbageCollector::Delete(domain_t domain, size_t offset) throw(interrupted_exception) {
    double beginTime = GetTime();
    LogInfo(logger) << (offset == 0 ? "Deleting domain " : "Resuming deletion of domain ") << domain;

    StorageIterator *iterator = storage->NewIterator(domain, offset);
    if (iterator != nullptr) {
        // TODO: iterate over domain and delete entries
        // check IsRunning at every cycle, if false -> throw interrupted_exception

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
    db->Delete(WriteOptions(), kPendingDeletionDataKey);
}
