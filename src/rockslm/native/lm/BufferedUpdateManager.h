//
// Created by Davide  Caroselli on 19/09/16.
//

#ifndef ROCKSLM_BUFFEREDUPDATEMANAGER_H
#define ROCKSLM_BUFFEREDUPDATEMANAGER_H

#include <cstddef>
#include <db/NGramStorage.h>
#include <mmt/IncrementalModel.h>
#include <mutex>
#include <condition_variable>
#include <boost/thread.hpp>

namespace rockslm {

    class BufferedUpdateManager {
    public:
        BufferedUpdateManager(db::NGramStorage *storage, size_t bufferSize, double maxDelay);

        ~BufferedUpdateManager();

        void Add(const updateid_t &id, const domain_t domain, const vector<wid_t> &sentence);

    private:
        db::NGramStorage *storage;

        db::NGramBatch *foregroundBatch;
        db::NGramBatch *backgroundBatch;

        mutex batchAccess;

        boost::thread *backgroundThread;
        mutex awakeMutex;
        condition_variable awakeCondition;
        double waitTimeout;
        bool stop;

        void AwakeBackgroundThread();

        void BackgroundThreadRun();
    };

}


#endif //ROCKSLM_BUFFEREDUPDATEMANAGER_H
