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

std::shared_ptr<StorageBucket> CorporaStorage::GetBucket(memory_t memory, bool putIfAbsent) {
    boost::upgrade_lock<boost::shared_mutex> lock(access);

    auto bucket = buckets.find(memory);
    if (bucket == buckets.end()) {
        boost::upgrade_to_unique_lock<boost::shared_mutex> uniqueLock(lock);

        StorageManifest::Entry mEntry;

        if (manifest->Get(memory, &mEntry, putIfAbsent)) {
            bucket = buckets.find(memory);

            if (bucket == buckets.end()) {
                fs::path filepath = folder / fs::path("_" + to_string(memory) + "_" + to_string(mEntry.seq_id));
                StorageBucket *bucketObj = new StorageBucket(filepath.string(), mEntry.size);

                bucket = buckets.emplace(memory, shared_ptr<StorageBucket>(bucketObj)).first;
            }
        }
    }

    return bucket == buckets.end() ? shared_ptr<StorageBucket>() : bucket->second;
}

bool CorporaStorage::Retrieve(memory_t memory, int64_t offset, std::vector<wid_t> *outSourceSentence,
                              std::vector<wid_t> *outTargetSentence, alignment_t *outAlignment) {
    std::shared_ptr<StorageBucket> bucket = GetBucket(memory);

    if (bucket == nullptr)
        return false;

    return bucket->Retrieve(offset, outSourceSentence, outTargetSentence, outAlignment) >= 0;
}

int64_t CorporaStorage::Append(memory_t memory, const std::vector<wid_t> &sourceSentence,
                               const std::vector<wid_t> &targetSentence,
                               const alignment_t &alignment) throw(storage_exception) {
    std::shared_ptr<StorageBucket> bucket = GetBucket(memory, true);
    int64_t result = bucket->Append(sourceSentence, targetSentence, alignment);

    pendingMemoriesAccess.lock();
    pendingMemories.insert(memory);
    pendingMemoriesAccess.unlock();

    return result;
}

void CorporaStorage::Flush() throw(storage_exception) {
    pendingMemoriesAccess.lock();

    for (auto memory = pendingMemories.begin(); memory != pendingMemories.end(); ++memory) {
        std::shared_ptr<StorageBucket> bucket = GetBucket(*memory);

        if (bucket != nullptr) {
            StorageManifest::Entry mEntry;
            manifest->Get(*memory, &mEntry, true);

            mEntry.size = bucket->Flush();
            manifest->Set(*memory, mEntry);
        }
    }

    pendingMemories.clear();
    pendingMemoriesAccess.unlock();
}

void CorporaStorage::Delete(memory_t memory) {
    std::shared_ptr<StorageBucket> bucket = GetBucket(memory);

    if (bucket != nullptr) {
        bucket->MarkForDeletion();

        pendingMemoriesAccess.lock();
        boost::upgrade_lock<boost::shared_mutex> lock(access);
        boost::upgrade_to_unique_lock<boost::shared_mutex> uniqueLock(lock);

        buckets.erase(memory);
        pendingMemories.erase(memory);

        StorageManifest::Entry mEntry;
        if (manifest->Get(memory, &mEntry)) {
            mEntry.seq_id++;
            mEntry.size = -1;

            manifest->Set(memory, mEntry);
        }

        pendingMemoriesAccess.unlock();
    }
}

StorageIterator *CorporaStorage::NewIterator(memory_t memory, size_t offset) {
    StorageIterator *iterator = nullptr;

    std::shared_ptr<StorageBucket> bucket = GetBucket(memory);
    if (bucket != nullptr)
        iterator = new StorageIterator(bucket, (int64_t) offset);

    return iterator;
}
