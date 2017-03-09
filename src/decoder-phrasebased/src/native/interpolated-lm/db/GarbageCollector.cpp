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
    // Deleted domains
    Iterator *it = db->NewIterator(ReadOptions());

    for (it->SeekToFirst(); it->Valid(); it->Next()) {
        Slice key = it->key();
        if (!HasDomainDeletionPrefix(key.data(), key.size()))
            break;

        domain_t domain = GetDomainFromDeletionKey(key.data(), key.size());
        if (domain > 0)
            queue.insert(domain);
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

void GarbageCollector::BackgroundThreadRun() {
    queueAccess.lock();
    unordered_set<domain_t> domains = queue;
    queueAccess.unlock();

    if (domains.empty())
        return;

    LogInfo(logger) << "Started cleaning process";
    double beginTime = GetTime();

    try {
        for (auto domain = domains.begin(); domain != domains.end(); ++domain) {
            Delete(*domain);

            db->Delete(WriteOptions(), MakeDomainDeletionKey(*domain));

            queueAccess.lock();
            queue.erase(*domain);
            queueAccess.unlock();
        }

        LogInfo(logger) << "Cleaning process completed in " << GetElapsedTime(beginTime) << "s";
    } catch (interrupted_exception &e) {
        LogInfo(logger) << "Cleaning process interrupted after " << GetElapsedTime(beginTime) << "s";
    }
}

void GarbageCollector::Delete(domain_t domain) throw(interrupted_exception) {
    if (!IsRunning())
        throw interrupted_exception();

    double beginTime = GetTime();
    LogInfo(logger) << "Deleting domain " << domain;

    WriteOptions writeOptions;

    string entryKey = MakeNGramKey(domain, 0);
    Iterator *it = db->NewIterator(ReadOptions());

    for (it->Seek(entryKey); it->Valid(); it->Next()) {
        Slice key = it->key();

        domain_t keyDomain;
        ngram_hash_t keyHash;
        GetNGramKeyData(key.data(), key.size(), &keyDomain, &keyHash);

        if (domain != keyDomain)
            break;

        db->Delete(writeOptions, key);
    }

    delete it;

    LogInfo(logger) << "Deletion of domain " << domain << " completed in " << GetElapsedTime(beginTime) << "s";
}
