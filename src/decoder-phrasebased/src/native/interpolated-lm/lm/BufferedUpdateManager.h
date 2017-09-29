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

            void Add(const updateid_t &id, const memory_t memory, const vector <wid_t> &sentence);

            void Delete(const updateid_t &id, const memory_t memory);

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
