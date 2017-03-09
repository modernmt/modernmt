//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_UPDATEMANAGER_H
#define SAPT_UPDATEMANAGER_H

#include <mutex>
#include <condition_variable>
#include <boost/thread.hpp>
#include <suffixarray/SuffixArray.h>
#include <util/BackgroundPollingThread.h>

namespace mmt {
    namespace sapt {

        class UpdateManager : public BackgroundPollingThread {
        public:
            UpdateManager(SuffixArray *index, size_t bufferSize, double maxDelay);

            virtual ~UpdateManager();

            void Add(const updateid_t &id, const domain_t domain, const vector<wid_t> &source,
                     const vector<wid_t> &target, const alignment_t &alignment);

            void Delete(const updateid_t &id, const domain_t domain);

        private:
            SuffixArray *index;

            UpdateBatch *foregroundBatch;
            UpdateBatch *backgroundBatch;

            mutex batchAccess;

            virtual void BackgroundThreadRun() override;
        };

    }
}


#endif //SAPT_UPDATEMANAGER_H
