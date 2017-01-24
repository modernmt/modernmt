//
// Created by Davide  Caroselli on 30/09/16.
//

#include "PostingList.h"
#include <algorithm>
#include <cstring>
#include <util/ioutils.h>
#include <util/hashutils.h>
#include <util/randutils.h>
#include <iostream>
#include <cassert>

using namespace mmt;
using namespace mmt::sapt;

PostingList::PostingList() : entryCount(0) {

}

void PostingList::Append(domain_t domain, const string &value) {
    assert(value.size() % kEntrySize == 0);

    size_t count = value.size() / kEntrySize;

    if (count > 0) {
        size_t size = value.size();
        const char *cstr = value.c_str();

        vector<char> chunk(size);
        memcpy(chunk.data(), cstr, size);

        datamap[domain] = chunk;
        entryCount += count;
    }
}

void PostingList::Append(domain_t domain, int64_t location, length_t offset) {
    vector<char> &data = datamap[domain];
    size_t ptr = data.size();

    data.resize(ptr + kEntrySize);
    WriteInt64(data.data(), &ptr, location);
    WriteUInt16(data.data(), &ptr, offset);

    entryCount++;
}

bool PostingList::empty() const {
    return datamap.empty();
}

size_t PostingList::size() const {
    return entryCount;
}

void PostingList::GetLocationMap(domain_t domain, unordered_map<int64_t, unordered_set<length_t>> &output) const {
    auto entry = datamap.find(domain);

    if (entry == datamap.end())
        return;

    output.reserve(entry->second.size() / kEntrySize);

    const char *bytes = entry->second.data();
    for (size_t i = 0; i < entry->second.size(); i += kEntrySize) {
        int64_t location = ReadInt64(bytes, i);
        length_t offset = ReadUInt16(bytes, i + 8);

        output[location].insert(offset);
    }
}

void PostingList::Retain(const PostingList *other, size_t start) {
    auto entry = datamap.begin();
    while (entry != datamap.end()) {
        bool deleteEntry;

        if (other->datamap.find(entry->first) == other->datamap.end()) {
            deleteEntry = true;
        } else {
            unordered_map<int64_t, unordered_set<length_t>> successors;
            other->GetLocationMap(entry->first, successors);

            size_t tail = 0;

            char *bytes = entry->second.data();
            for (size_t i = 0; i < entry->second.size(); i += kEntrySize) {
                int64_t location = ReadInt64(bytes, i);
                length_t offset = ReadUInt16(bytes, i + 8);

                bool remove;

                auto successor = successors.find(location);
                if (successor == successors.end()) {
                    remove = true;
                } else {
                    unordered_set<length_t> &successors_offsets = successor->second;
                    remove = successors_offsets.find((length_t) (offset + start)) == successors_offsets.end();
                }

                if (!remove) {
                    memcpy(&bytes[tail], &bytes[i], kEntrySize);
                    tail += kEntrySize;
                } else {
                    entryCount--;
                }
            }

            deleteEntry = (tail == 0);
            entry->second.resize(tail);
        }

        if (deleteEntry) {
            entryCount -= entry->second.size() / kEntrySize;
            entry = datamap.erase(entry);
        } else {
            ++entry;
        }
    }
}

string PostingList::Serialize() const {
    string buffer;

    for (auto entry = datamap.begin(); entry != datamap.end(); ++entry)
        buffer.append(entry->second.data(), entry->second.size());

    return buffer;
}

void PostingList::Deserialize(const string& string, vector<location_t> &output) {
    output.clear();

    const char *bytes = string.data();
    for (size_t i = 0; i < string.size(); i += kEntrySize) {
        int64_t location = ReadInt64(bytes, i);
        length_t offset = ReadUInt16(bytes, i + 8);

        output.push_back(location_t(location, offset));
    }
}

void PostingList::GetLocations(vector<location_t> &output, size_t limit, unsigned int seed) {
    if (empty())
        return;

    output.reserve(limit == 0 ? size() : limit);

    if (limit == 0 || size() <= limit) {
        // Collect all
        for (auto entry = datamap.begin(); entry != datamap.end(); ++entry) {
            for (size_t i = 0; i < entry->second.size(); i += kEntrySize) {
                int64_t location;
                length_t offset;

                Get(entry->second, i, &location, &offset);
                output.push_back(location_t(location, offset, entry->first));
            }
        }
    } else {
        if (seed == 0)
            seed = (unsigned int) time(NULL);

        vector<size_t> sequence;
        GenerateRandomSequence(size(), limit, seed, sequence);
        sort(sequence.begin(), sequence.end());

        auto sequencePtr = sequence.begin();
        size_t dataOffset = 0;


        for (auto entry = datamap.begin(); entry != datamap.end() && sequencePtr != sequence.end(); ++entry) {//
            size_t i;

            while ((i = (*sequencePtr) * kEntrySize) < dataOffset + entry->second.size()
                   && sequencePtr != sequence.end()) {
                int64_t location;
                length_t offset;

                Get(entry->second, i - dataOffset, &location, &offset);
                output.push_back(location_t(location, offset, entry->first));

                sequencePtr++;
            }

            dataOffset += entry->second.size();
        }
    }
}

inline void PostingList::Get(const vector<char> &chunk, size_t index, int64_t *outLocation, length_t *outOffset) {
    const char *bytes = chunk.data();
    *outLocation = ReadInt64(bytes, index);
    *outOffset = ReadUInt16(bytes, index + 8);
}
