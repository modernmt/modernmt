//
// Created by Davide  Caroselli on 19/09/16.
//

#ifndef ILM_BUFFEREDUPDATEMANAGER_H
#define ILM_BUFFEREDUPDATEMANAGER_H

#include <cstddef>
#include <db/NGramStorage.h>
#include <mmt/IncrementalModel.h>
#include <mutex>
#include <condition_variable>
#include <boost/thread.hpp>

namespace mmt {
    namespace ilm {

        class BufferedUpdateManager {
        public:
            BufferedUpdateManager(NGramStorage *storage, size_t bufferSize, double maxDelay);

            ~BufferedUpdateManager();

            void Add(const updateid_t &id, const domain_t domain, const vector <wid_t> &sentence);

        private:
            NGramStorage *storage;

            NGramBatch *foregroundBatch;
            NGramBatch *backgroundBatch;

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


#endif //ILM_BUFFEREDUPDATEMANAGER_H
