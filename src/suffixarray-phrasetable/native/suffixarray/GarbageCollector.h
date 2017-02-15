//
// Created by Davide  Caroselli on 14/02/17.
//

#ifndef SAPT_GARBAGECOLLECTOR_H
#define SAPT_GARBAGECOLLECTOR_H

#include <vector>
#include <mmt/sentence.h>
#include <mutex>
#include <rocksdb/db.h>
#include <boost/thread.hpp>
#include <util/BackgroundPollingThread.h>
#include <unordered_set>
#include <mmt/logging/Logger.h>
#include "CorpusStorage.h"

namespace mmt {
    namespace sapt {

        class GarbageCollector : public BackgroundPollingThread {
        public:
            GarbageCollector(CorpusStorage *storage, rocksdb::DB *db, const std::unordered_set<domain_t> &domains,
                             size_t batchSize, double timeout);

            virtual ~GarbageCollector();

            void MarkForDeletion(const std::vector<domain_t> &domains);

            std::unordered_set<domain_t> GetDomainsMarkedForDeletion();

        private:
            mmt::logging::Logger logger;

            rocksdb::DB *db;
            CorpusStorage *storage;

            domain_t pendingDeletionDomain;
            size_t pendingDeletionOffset;
            size_t batchSize;

            std::mutex queueAccess;
            std::unordered_set<domain_t> queue;

            class interrupted_exception : public exception {
            public:
                interrupted_exception() {};
            };

            void BackgroundThreadRun() override;

            void Delete(domain_t domain, size_t offset = 0) throw(interrupted_exception);

            void DeleteStorage(domain_t domain);
        };

    }
}

#endif //SAPT_GARBAGECOLLECTOR_H
