//
// Created by Davide  Caroselli on 30/09/16.
//

#include "PostingList.h"
#include <cstring>
#include <util/ioutils.h>
#include <util/hashutils.h>
#include <util/randutils.h>
#include <iostream>

using namespace mmt;
using namespace mmt::sapt;

const static size_t kEntrySize = sizeof(int64_t) + sizeof(length_t);

static inline void AppendToVector(vector<char> &dest, const char *_data, size_t size) {
    dest.resize(dest.size() + size);
    memcpy(&dest[dest.size() - size], _data, size);
}

PostingList::PostingList() : phraseHash(0) {

}

PostingList::PostingList(const vector<wid_t> &phrase) : phraseHash(words_hash(phrase)) {
}

PostingList::PostingList(const vector<wid_t> &sentence, size_t offset, size_t size)
        : phraseHash(words_hash(sentence, offset, size)) {
}

void PostingList::Append(domain_t domain, const char *_data, size_t size, const unordered_set<int64_t> *filterBy) {
    if (filterBy && !filterBy->empty()) {
        data.reserve(data.size() + size);

        for (size_t i = 0; i < size; i += kEntrySize) {
            int64_t location = ReadInt64(_data, i);

            if (filterBy->find(location) == filterBy->end())
                AppendToVector(data, &_data[i], kEntrySize);
        }
    } else {
        AppendToVector(data, _data, size);
    }

    if (domains.empty() || domains[domains.size() - 1].first != domain) {
        domains.push_back(make_pair(domain, size));
    } else {
        domains[domains.size() - 1].second += size;
    }
}

void PostingList::Append(domain_t domain, int64_t location, length_t offset) {
    size_t ptr = data.size();

    data.resize(ptr + kEntrySize);
    WriteInt64(data.data(), &ptr, location);
    WriteUInt32(data.data(), &ptr, offset);

    if (domains.empty() || domains[domains.size() - 1].first != domain) {
        domains.push_back(make_pair(domain, kEntrySize));
    } else {
        domains[domains.size() - 1].second += kEntrySize;
    }
}

void PostingList::Append(const PostingList &other) {
    if (other.phraseHash != phraseHash)
        throw invalid_argument("Cannot join PostingLists with different phrases");

    AppendToVector(data, other.data.data(), other.data.size());
    domains.insert(domains.end(), other.domains.cbegin(), other.domains.cend());
}

unordered_set<int64_t> PostingList::GetLocations() const {
    unordered_set<int64_t> locations;

    const char *_data = data.data();
    for (size_t i = 0; i < data.size(); i += kEntrySize)
        locations.insert(ReadInt64(_data, i));

    return locations;
}

size_t PostingList::size() const {
    return data.size() / kEntrySize;
}

bool PostingList::empty() const {
    return data.empty();
}

string PostingList::Serialize() const {
    return string(data.data(), data.size());
}

map<int64_t, pair<domain_t, vector<length_t>>> PostingList::GetSamples(size_t limit, unsigned int shuffleSeed) const {
    map<int64_t, pair<domain_t, vector<length_t>>> result;

    if (!empty()) {
        if (limit == 0 || size() <= limit || shuffleSeed == 0) {
            size_t size_limit = data.size();
            if (limit > 0)
                size_limit = std::min(limit * kEntrySize, size_limit);

            CollectAll(size_limit, result);
        } else {
            // Domain report is not supported when shuffle is true for
            // performance issues. This is not a problem for the
            // current implementation of the SuffixArrays.
            // (Shuffled sampling is requested only on background model
            // and its domain is always 0)

            if (phraseHash == 0)
                throw runtime_error("Phrase hash cannot be zero");

            vector<size_t> sequence;
            GenerateRandomSequence(size(), limit, phraseHash, sequence);

            const char *_data = data.data();
            for (auto index = sequence.begin(); index != sequence.end(); ++index) {
                size_t ptr = (*index) * kEntrySize;

                int64_t location = ReadInt64(_data, &ptr);
                length_t offset = ReadUInt16(_data, &ptr);

                pair<domain_t, vector<length_t>> &entry = result[location];
                entry.first = 0; // no domain
                entry.second.push_back(offset);
            }
        }
    }

    return result;
}

void PostingList::CollectAll(size_t size_limit, map<int64_t, pair<domain_t, vector<length_t>>> &output) const {
    auto domain_ptr = domains.begin();
    size_t domain_offset = 0;

    const char *_data = data.data();
    for (size_t i = 0; i < size_limit; i += kEntrySize) {
        int64_t location = ReadInt64(_data, i);
        length_t offset = ReadUInt16(_data, i + 8);

        if (domain_ptr != domains.end() && i > domain_offset + domain_ptr->second) {
            domain_offset += domain_ptr->second;
            domain_ptr++;
        }

        pair<domain_t, vector<length_t>> &value = output[location];
        value.first = domain_ptr->first;
        value.second.push_back(offset);
    }
}

void PostingList::Retain(const PostingList &_successors, length_t start) {
    unordered_map<int64_t, unordered_set<length_t>> successors;
    _successors.GetLocationMap(successors);

    size_t tail = 0;

    char *_data = data.data();
    for (size_t i = 0; i < data.size(); i += kEntrySize) {
        int64_t location = ReadInt64(_data, i);
        length_t offset = ReadUInt16(_data, i + 8);

        bool remove;

        auto successor = successors.find(location);
        if (successor == successors.end()) {
            remove = true;
        } else {
            unordered_set<length_t> &successors_offsets = successor->second;
            remove = successors_offsets.find(offset + start) == successors_offsets.end();
        }

        if (!remove) {
            memcpy(&_data[tail], &_data[i], kEntrySize);
            tail += kEntrySize;
        }
    }

    data.resize(tail);
}

void PostingList::GetLocationMap(unordered_map<int64_t, unordered_set<length_t>> &output) const {
    const char *_data = data.data();
    for (size_t i = 0; i < data.size(); i += kEntrySize) {
        int64_t location = ReadInt64(_data, i);
        length_t offset = ReadUInt16(_data, i + 8);

        output[location].insert(offset);
    }
}
