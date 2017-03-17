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
#include <suffixarray/storage/CorporaStorage.h>
#include "index_exception.h"

namespace mmt {
    namespace sapt {

        class GarbageCollector : public BackgroundPollingThread {
        public:
            GarbageCollector(CorporaStorage *storage, rocksdb::DB *db,
                             uint8_t prefixLength, size_t batchSize, double timeout);

            virtual ~GarbageCollector();

            void MarkForDeletion(const std::vector<domain_t> &domains);

        private:
            mmt::logging::Logger logger;

            rocksdb::DB *db;
            CorporaStorage *storage;

            domain_t pendingDeletionDomain;
            int64_t pendingDeletionOffset;
            size_t batchSize;
            uint8_t prefixLength;

            std::mutex queueAccess;
            std::unordered_set<domain_t> queue;

            class interrupted_exception : public std::exception {
            public:
                interrupted_exception() {};
            };

            void BackgroundThreadRun() throw(index_exception) override;

            void Delete(domain_t domain, int64_t offset = 0) throw(interrupted_exception, index_exception);

            void DeleteStorage(domain_t domain) throw(index_exception);

            int64_t LoadBatch(domain_t domain, StorageIterator *iterator,
                              std::unordered_set<std::string> *outPrefixKeys,
                              std::unordered_map<std::string, int64_t> *outTargetCounts) throw(interrupted_exception);

            void WriteBatch(domain_t domain, int64_t offset, const std::unordered_set<std::string> &prefixKeys,
                            const std::unordered_map<std::string, int64_t> &targetCounts);

        };

    }
}

#endif //SAPT_GARBAGECOLLECTOR_H
