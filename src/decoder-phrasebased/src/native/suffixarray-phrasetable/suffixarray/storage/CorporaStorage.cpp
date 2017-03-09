//
// Created by Davide  Caroselli on 15/02/17.
//

#include <boost/filesystem/operations.hpp>
#include <mmt/logging/Logger.h>
#include "CorporaStorage.h"

namespace fs = boost::filesystem;

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

CorporaStorage::CorporaStorage(const std::string &folder, StorageManifest *manifest) throw(storage_exception)
        : folder(fs::absolute(fs::path(folder))), manifest(manifest) {
    if (!fs::is_directory(this->folder))
        fs::create_directory(this->folder);
}

CorporaStorage::~CorporaStorage() {
    delete manifest;
}

std::shared_ptr<StorageBucket> CorporaStorage::GetBucket(domain_t domain, bool putIfAbsent) {
    boost::upgrade_lock<boost::shared_mutex> lock(access);

    auto bucket = buckets.find(domain);
    if (bucket == buckets.end()) {
        boost::upgrade_to_unique_lock<boost::shared_mutex> uniqueLock(lock);

        StorageManifest::Entry mEntry;

        if (manifest->Get(domain, &mEntry, putIfAbsent)) {
            bucket = buckets.find(domain);

            if (bucket == buckets.end()) {
                fs::path filepath = folder / fs::path("_" + to_string(domain) + "_" + to_string(mEntry.seq_id));
                StorageBucket *bucketObj = new StorageBucket(filepath.string(), mEntry.size);

                bucket = buckets.emplace(domain, shared_ptr<StorageBucket>(bucketObj)).first;
            }
        }
    }

    return bucket == buckets.end() ? shared_ptr<StorageBucket>() : bucket->second;
}

bool CorporaStorage::Retrieve(domain_t domain, int64_t offset, std::vector<wid_t> *outSourceSentence,
                              std::vector<wid_t> *outTargetSentence, alignment_t *outAlignment) {
    std::shared_ptr<StorageBucket> bucket = GetBucket(domain);

    if (bucket == nullptr)
        return false;

    return bucket->Retrieve(offset, outSourceSentence, outTargetSentence, outAlignment) >= 0;
}

int64_t CorporaStorage::Append(domain_t domain, const std::vector<wid_t> &sourceSentence,
                               const std::vector<wid_t> &targetSentence,
                               const alignment_t &alignment) throw(storage_exception) {
    std::shared_ptr<StorageBucket> bucket = GetBucket(domain, true);
    int64_t result = bucket->Append(sourceSentence, targetSentence, alignment);

    pendingDomainsAccess.lock();
    pendingDomains.insert(domain);
    pendingDomainsAccess.unlock();

    return result;
}

void CorporaStorage::Flush() throw(storage_exception) {
    pendingDomainsAccess.lock();

    for (auto domain = pendingDomains.begin(); domain != pendingDomains.end(); ++domain) {
        std::shared_ptr<StorageBucket> bucket = GetBucket(*domain);

        if (bucket != nullptr) {
            StorageManifest::Entry mEntry;
            manifest->Get(*domain, &mEntry, true);

            mEntry.size = bucket->Flush();
            manifest->Set(*domain, mEntry);
        }
    }

    pendingDomains.clear();
    pendingDomainsAccess.unlock();
}

void CorporaStorage::Delete(domain_t domain) {
    std::shared_ptr<StorageBucket> bucket = GetBucket(domain);

    if (bucket != nullptr) {
        bucket->MarkForDeletion();

        pendingDomainsAccess.lock();
        boost::upgrade_lock<boost::shared_mutex> lock(access);
        boost::upgrade_to_unique_lock<boost::shared_mutex> uniqueLock(lock);

        buckets.erase(domain);
        pendingDomains.erase(domain);

        StorageManifest::Entry mEntry;
        if (manifest->Get(domain, &mEntry)) {
            mEntry.seq_id++;
            mEntry.size = -1;

            manifest->Set(domain, mEntry);
        }

        pendingDomainsAccess.unlock();
    }
}

StorageIterator *CorporaStorage::NewIterator(domain_t domain, size_t offset) {
    StorageIterator *iterator = nullptr;

    std::shared_ptr<StorageBucket> bucket = GetBucket(domain);
    if (bucket != nullptr)
        iterator = new StorageIterator(bucket, (int64_t) offset);

    return iterator;
}
