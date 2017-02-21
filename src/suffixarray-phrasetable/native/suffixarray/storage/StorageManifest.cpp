//
// Created by Davide  Caroselli on 15/02/17.
//

#include "StorageManifest.h"
#include <util/ioutils.h>

static_assert(sizeof(mmt::domain_t) == 4, "Current implementation works only with 32-bit domain id");

static const size_t kEntrySize = sizeof(mmt::domain_t) + sizeof(uint16_t) + sizeof(int64_t);

using namespace std;
using namespace mmt;
using namespace mmt::sapt;

StorageManifest::StorageManifest() {
}

StorageManifest::StorageManifest(const std::unordered_map<domain_t, Entry> &entries) : entries(entries) {
}

StorageManifest *StorageManifest::Deserialize(const char *bytes, size_t bytesCount) throw(storage_exception) {
    if (bytes == nullptr || bytesCount == 0)
        return new StorageManifest();

    if (bytesCount % kEntrySize != 0)
        throw storage_exception("Invalid manifest data length: " + to_string(bytesCount));

    unordered_map<domain_t, Entry> data;

    size_t ptr = 0;
    while (ptr < bytesCount) {
        domain_t domain = ReadUInt32(bytes, &ptr);
        uint16_t e_seqid = ReadUInt16(bytes, &ptr);
        int64_t e_size = ReadInt64(bytes, &ptr);

        data[domain] = Entry(e_seqid, e_size);
    }

    return new StorageManifest(data);
}

std::string StorageManifest::Serialize() const {
    if (entries.empty()) {
        return string();
    } else {
        size_t size = entries.size() * kEntrySize;

        char *bytes = new char[size];

        size_t ptr = 0;
        for (auto entry = entries.begin(); entry != entries.end(); ++entry) {
            WriteUInt32(bytes, &ptr, entry->first);
            WriteUInt16(bytes, &ptr, entry->second.seq_id);
            WriteInt64(bytes, &ptr, entry->second.size);
        }

        string result(bytes, size);
        delete[] bytes;

        return result;
    }
}

bool StorageManifest::Get(domain_t domain, StorageManifest::Entry *outEntry, bool putIfAbsent) {
    auto entry = entries.find(domain);
    if (entry == entries.end() && putIfAbsent)
        entry = entries.emplace(domain, Entry()).first;

    if (entry != entries.end()) {
        *outEntry = entry->second;
        return true;
    } else {
        return false;
    }
}

void StorageManifest::Set(domain_t domain, const StorageManifest::Entry &entry) {
    entries[domain] = entry;
}

void StorageManifest::GetDomains(std::unordered_set<domain_t> *outDomains) const {
    outDomains->clear();

    for (auto entry = entries.begin(); entry != entries.end(); ++entry)
        outDomains->insert(entry->first);
}
