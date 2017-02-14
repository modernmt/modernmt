//
// Created by Davide  Caroselli on 14/02/17.
//

#include "GarbageCollector.h"

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

GarbageCollector::GarbageCollector(rocksdb::DB *db, const std::unordered_set<domain_t> &domains, double timeout)
        : BackgroundPollingThread(timeout), db(db), queue(domains) {
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


    //TODO: delete
}
