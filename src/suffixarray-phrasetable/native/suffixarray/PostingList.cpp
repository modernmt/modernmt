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

const static size_t kEntrySize = sizeof(int64_t) + sizeof(length_t);

PostingList::PostingList() : phraseHash(0), entryCount(0) {

}

PostingList::PostingList(const vector<wid_t> &sentence, size_t offset, size_t size)
        : phraseHash(words_hash(sentence, offset, size)), entryCount(0) {
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
}

void PostingList::Join(const PostingList &other) {
    assert(other.phraseHash == phraseHash);

    datamap.insert(other.datamap.cbegin(), other.datamap.cend());
    entryCount += other.entryCount;
}

bool PostingList::empty() const {
    return datamap.empty();
}

size_t PostingList::size() const {
    return entryCount;
}

void PostingList::GetLocationMap(unordered_map<int64_t, unordered_set<length_t>> &output) const {
    output.reserve(size());

    for (auto entry = datamap.begin(); entry != datamap.end(); ++entry) {
        const char *bytes = entry->second.data();

        for (size_t i = 0; i < entry->second.size(); i += kEntrySize) {
            int64_t location = ReadInt64(bytes, i);
            length_t offset = ReadUInt16(bytes, i + 8);

            output[location].insert(offset);
        }
    }
}

void PostingList::Retain(const PostingList &other, length_t start) {
    assert(datamap.size() == 1);
    assert(other.datamap.size() == 1);
    assert(datamap.begin()->first == other.datamap.begin()->first);

    unordered_map<int64_t, unordered_set<length_t>> successors;
    other.GetLocationMap(successors);

    auto entry = datamap.begin();
    while (entry != datamap.end()) {
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
                remove = successors_offsets.find(offset + start) == successors_offsets.end();
            }

            if (!remove) {
                // TODO: Possibilità di speedup che dovrebbe vedersi in n-gram di ordine maggiore
                // evitare numerosi piccoli append, piuttosto cercare di raggrupparli
                // tenendo in memoria l'ultima posizione copiata (copyBegin e copyEnd)
                memcpy(&bytes[tail], &bytes[i], kEntrySize);
                tail += kEntrySize;
            }
        }

        if (tail == 0) {
            entry = datamap.erase(entry);
        } else {
            entry->second.resize(tail);
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

void PostingList::GetSamples(samplemap_t &output, size_t limit) {
    if (empty())
        return;

    if (limit == 0 || size() <= limit) {
        // Collect all
        for (auto entry = datamap.begin(); entry != datamap.end(); ++entry) {
            unordered_map<int64_t, vector<length_t>> &outEntry = output[entry->first];

            for (size_t i = 0; i < entry->second.size(); i += kEntrySize) {
                int64_t location;
                length_t offset;

                Get(entry->second, i, &location, &offset);
                outEntry[location].push_back(offset);
            }
        }
    } else {
        assert(phraseHash != 0);

        vector<size_t> sequence;
        GenerateRandomSequence(size(), limit, phraseHash, sequence);
        sort(sequence.begin(), sequence.end());

        auto sequencePtr = sequence.begin();
        size_t dataOffset = 0;

        for (auto entry = datamap.begin(); entry != datamap.end() && sequencePtr != sequence.end(); ++entry) {
            unordered_map<int64_t, vector<length_t>> *outEntry = NULL;

            size_t i = (*sequencePtr) * kEntrySize;
            if (i < dataOffset + entry->second.size())
                outEntry = &(output[entry->first]);

            while (i < dataOffset + entry->second.size() && sequencePtr != sequence.end()) {
                int64_t location;
                length_t offset;

                Get(entry->second, i - dataOffset, &location, &offset);
                (*outEntry)[location].push_back(offset);

                sequencePtr++;
                i = (*sequencePtr) * kEntrySize;
            }

            dataOffset += entry->second.size();
        }
    }
}

void PostingList::Get(const vector<char> &chunk, size_t index, int64_t *outLocation, length_t *outOffset) {
    const char *bytes = chunk.data();
    *outLocation = ReadInt64(bytes, index);
    *outOffset = ReadUInt16(bytes, index + 8);
}
