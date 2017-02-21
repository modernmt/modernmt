//
// Created by Davide  Caroselli on 17/02/17.
//

#ifndef ILM_GARBAGECOLLECTOR_H
#define ILM_GARBAGECOLLECTOR_H

#include <mmt/logging/Logger.h>
#include <util/BackgroundPollingThread.h>
#include <rocksdb/db.h>
#include <mmt/sentence.h>
#include <unordered_set>

namespace mmt {
    namespace ilm {

        class GarbageCollector : public BackgroundPollingThread {
        public:
            GarbageCollector(rocksdb::DB *db, double timeout);

            virtual ~GarbageCollector();

            void MarkForDeletion(const std::vector<domain_t> &domains);

        private:
            mmt::logging::Logger logger = logging::Logger("ilm.GarbageCollector");

            rocksdb::DB *db;

            std::mutex queueAccess;
            std::unordered_set<domain_t> queue;

            class interrupted_exception : public std::exception {
            public:
                interrupted_exception() {};
            };

            void BackgroundThreadRun() override;

            void Delete(domain_t domain) throw(interrupted_exception);

        };

    }
}

#endif //ILM_GARBAGECOLLECTOR_H
