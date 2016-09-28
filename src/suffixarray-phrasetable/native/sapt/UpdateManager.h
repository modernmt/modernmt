//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_UPDATEMANAGER_H
#define SAPT_UPDATEMANAGER_H

#include <mutex>
#include <condition_variable>
#include <boost/thread.hpp>
#include "CorpusStorage.h"
#include "CorpusIndex.h"

namespace mmt {
    namespace sapt {

        class UpdateManager {
        public:
            UpdateManager(CorpusStorage *storage, CorpusIndex *index, size_t bufferSize, double maxDelay);

            ~UpdateManager();

            void Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                     const std::vector<wid_t> &target, const alignment_t &alignment);

        private:
            CorpusStorage *storage;
            CorpusIndex *index;

            UpdateBatch *foregroundBatch;
            UpdateBatch *backgroundBatch;

            mutex batchAccess;

            boost::thread *backgroundThread;
            mutex awakeMutex;
            condition_variable awakeCondition;
            double waitTimeout;
            bool stop;

            void AwakeBackgroundThread(bool wait);

            void BackgroundThreadRun();
        };

    }
}


#endif //SAPT_UPDATEMANAGER_H
