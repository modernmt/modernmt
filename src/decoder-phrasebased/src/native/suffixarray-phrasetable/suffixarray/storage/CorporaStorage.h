//
// Created by Davide  Caroselli on 15/02/17.
//

#ifndef SAPT_CORPORASTORAGE_H
#define SAPT_CORPORASTORAGE_H

#include <exception>
#include <string>
#include <mutex>
#include <memory>
#include <mmt/sentence.h>
#include <unordered_set>
#include <boost/thread/shared_mutex.hpp>
#include <boost/filesystem/path.hpp>
#include "StorageIterator.h"
#include "StorageManifest.h"
#include "StorageBucket.h"

namespace mmt {
    namespace sapt {

        class CorporaStorage {
        public:
            CorporaStorage(const std::string &folder, StorageManifest *manifest) throw(storage_exception);

            ~CorporaStorage();

            bool Retrieve(domain_t domain, int64_t offset,
                          std::vector<wid_t> *outSourceSentence, std::vector<wid_t> *outTargetSentence,
                          alignment_t *outAlignment);

            int64_t Append(domain_t domain, const std::vector<wid_t> &sourceSentence,
                           const std::vector<wid_t> &targetSentence,
                           const alignment_t &alignment) throw(storage_exception);

            void Flush() throw(storage_exception);

            void Delete(domain_t domain);

            StorageIterator *NewIterator(domain_t domain, size_t offset = 0);

            const StorageManifest *GetManifest() const {
                return manifest;
            }

        private:
            const boost::filesystem::path folder;

            StorageManifest *manifest;
            std::unordered_map<domain_t, std::shared_ptr<StorageBucket>> buckets;
            std::unordered_set<domain_t> pendingDomains;

            std::mutex pendingDomainsAccess;
            boost::shared_mutex access;

            std::shared_ptr<StorageBucket> GetBucket(domain_t domain, bool putIfAbsent = false);
        };

    }
}


#endif //SAPT_CORPORASTORAGE_H
