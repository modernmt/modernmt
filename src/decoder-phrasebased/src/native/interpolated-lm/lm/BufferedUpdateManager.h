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
#include <util/BackgroundPollingThread.h>

namespace mmt {
    namespace ilm {

        class BufferedUpdateManager : public BackgroundPollingThread {
        public:
            BufferedUpdateManager(NGramStorage *storage, size_t bufferSize, double maxDelay);

            ~BufferedUpdateManager();

            void Add(const update_batch_t &batch);

        private:
            NGramStorage *storage;

            NGramBatch *foregroundBatch;
            NGramBatch *backgroundBatch;

            mutex batchAccess;

            virtual void BackgroundThreadRun() override;
        };

    }
}


#endif //ILM_BUFFEREDUPDATEMANAGER_H
