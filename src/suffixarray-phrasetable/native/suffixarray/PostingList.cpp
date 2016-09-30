//
// Created by Davide  Caroselli on 30/09/16.
//

#include "PostingList.h"
#include <util/ioutils.h>
#include <random>
#include <algorithm>

using namespace mmt;
using namespace mmt::sapt;

const static size_t kEntrySize = sizeof(int64_t) + sizeof(length_t);

static inline void AppendToVector(vector<char> &dest, const char *_data, size_t size) {
    dest.resize(dest.size() + size);
    memcpy(&dest[dest.size() - size], _data, size);
}

void PostingList::Append(domain_t domain, const char *_data, size_t size, const unordered_set<int64_t> *filterBy) {
    if (filterBy) {
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
    AppendToVector(data, other.data.data(), other.data.size());
    domains.insert(domains.end(), other.domains.begin(), other.domains.end());
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
    if (empty())
        return map<int64_t, pair<domain_t, vector<length_t>>>();

    if (limit == 0 || size() <= limit || shuffleSeed == 0) {
        return CollectAll(std::min(limit * kEntrySize, data.size()));
    } else {
        // Domain report is not supported when shuffle is true for
        // performance issues. This is not a problem for the
        // current implementation of the SuffixArrays.
        // (Shuffled sampling is requested only on background model
        // and its domain is always 0)

        unordered_set<size_t> coveredPositions;

        while (coveredPositions.size() < limit) {

        }
//
//
//    vector<int64_t> keys;
//    keys.reserve(locations.size());
//
//    for (auto &it : positions) {
//        keys.push_back(it.first);
//    }
//    cerr << "SuffixArray::Retrieve collect keys took " << GetElapsedTime(begin) << "s" << endl;
//
//    begin = GetTime();
//    // Limit result
//    if (limit > 0 && positions.size() > limit) {
//        sort(keys.begin(), keys.end());
//
//        unsigned int seed = 3874556238;
//
//        shuffle(keys.begin(), keys.end(), default_random_engine(seed));
//
//        keys.resize(limit);
//    }
//    }
}

map<int64_t, pair<domain_t, vector<length_t>>> PostingList::CollectAll(size_t size_limit) const {
    map<int64_t, pair<domain_t, vector<length_t>>> result;

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

        pair<domain_t, vector<length_t>> &value = result[location];
        value.first = domain_ptr->first;
        value.second.push_back(offset);
    }

    return result;
}

//static void Retain(positionsmap_t &map, const positionsmap_t &successors, length_t offset) {
//    auto it = map.begin();
//    while (it != map.end()) {
//        bool remove;
//
//        auto successor = successors.find(it->first);
//        if (successor == successors.end()) {
//            remove = true;
//        } else {
//            vector<length_t> &start_positions = it->second.second;
//            const vector<length_t> &successors_offsets = successor->second.second;
//
//            start_positions.erase(
//                    remove_if(start_positions.begin(), start_positions.end(),
//                              [successors_offsets, offset](length_t start) {
//                                  if (offset > start)
//                                      return false;
//
//                                  auto e = find(successors_offsets.begin(), successors_offsets.end(), start - offset);
//                                  return e != successors_offsets.end();
//                              }),
//                    start_positions.end()
//            );
//
//            remove = start_positions.empty();
//        }
//
//        if (remove)
//            it = map.erase(it);
//        else
//            it++;
//    }
//}
