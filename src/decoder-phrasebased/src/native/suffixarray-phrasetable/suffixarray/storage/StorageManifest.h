//
// Created by Davide  Caroselli on 15/02/17.
//

#ifndef SAPT_STORAGEMANIFEST_H
#define SAPT_STORAGEMANIFEST_H

#include <cstddef>
#include <unordered_map>
#include <mmt/sentence.h>
#include <unordered_set>
#include "storage_exception.h"

namespace mmt {
    namespace sapt {

        class StorageManifest {
        public:
            struct Entry {
                uint16_t seq_id;
                int64_t size;

                Entry(uint16_t seq_id = 0, int64_t size = -1) : seq_id(seq_id), size(size) {};
            };

            static StorageManifest *Deserialize(const char *bytes, size_t bytesCount) throw(storage_exception);

            StorageManifest();

            std::string Serialize() const;

            void GetMemories(std::unordered_set<memory_t> *outMemories) const;

            bool Get(memory_t memory, Entry *outEntry, bool putIfAbsent = false);

            void Set(memory_t memory, const Entry &entry);

        private:
            std::unordered_map<memory_t, Entry> entries;

            StorageManifest(const std::unordered_map<memory_t, Entry> &entries);
        };

    }
}

#endif //SAPT_STORAGEMANIFEST_H
