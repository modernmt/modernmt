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

namespace mmt {
    namespace sapt {

        class GarbageCollector: public BackgroundPollingThread {
        public:
            GarbageCollector(rocksdb::DB *db, const std::unordered_set<domain_t> &domains, double timeout = 120.);

            virtual ~GarbageCollector();

            void MarkForDeletion(const std::vector<domain_t> &domains);

            std::unordered_set<domain_t> GetDomainsMarkedForDeletion();

        private:
            rocksdb::DB *db;

            std::mutex queueAccess;
            std::unordered_set<domain_t> queue;

            void BackgroundThreadRun() override;
        };

    }
}

#endif //SAPT_GARBAGECOLLECTOR_H
