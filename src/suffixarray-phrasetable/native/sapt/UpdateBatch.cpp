//
// Created by Davide  Caroselli on 27/09/16.
//

#include "UpdateBatch.h"
#include "CorpusStorage.h"

using namespace mmt::sapt;

UpdateBatch::UpdateBatch(uint8_t prefixLength, size_t maxSize, const vector<seqid_t> &_streams) :
        prefixLength(prefixLength), maxSize(maxSize), currentOffset(0), size(0) {
    streams = _streams;
}

void UpdateBatch::Clear() {
    encodedData.clear();
    prefixes.clear();
    currentOffset = 0;
    size = 0;
}

void UpdateBatch::Reset(const vector<seqid_t> &_streams) {
    streams = _streams;
    Clear();
}

bool UpdateBatch::SetStreamIfValid(stream_t stream, seqid_t sentence) {
    if (streams.size() <= stream)
        streams.resize(stream + 1, 0);

    if (streams[stream] < sentence) {
        streams[stream] = sentence;
        return true;
    } else {
        return false;
    }
}

bool UpdateBatch::Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                      const std::vector<wid_t> &target, const alignment_t &alignment) {
    if (size >= maxSize)
        return false;

    if (!SetStreamIfValid(id.stream_id, id.sentence_id))
        return true;

    AddToBatch(domain, source, target, alignment);

    return true;
}

bool UpdateBatch::Add(const domain_t domain, const std::vector<wid_t> &source,
                      const std::vector<wid_t> &target, const alignment_t &alignment) {
    if (size >= maxSize)
        return false;

    AddToBatch(domain, source, target, alignment);

    return true;
}

void UpdateBatch::AddToBatch(const domain_t domain, const std::vector<wid_t> &source,
                             const std::vector<wid_t> &target, const alignment_t &alignment) {
    auto el = prefixes.emplace(domain, prefixmap_t());
    prefixmap_t &prefixmap = el.first->second;

    // Add prefixes
    for (length_t start = 0; start < source.size(); ++start) {
        size_t length = prefixLength;
        if (start + length > source.size())
            length = source.size() - start;

        vector<wid_t> prefix(length);
        for (length_t i = 0; i < length; ++i)
            prefix[i] = source[start + i];

        auto e = prefixmap.emplace(prefix, vector<position_t>());
        e.first->second.push_back(position_t(currentOffset, start));

        if (e.second)
            this->size++;
    }

    // Add encoded data
    vector<char> encoded;
    CorpusStorage::Encode(source, target, alignment, &encoded);

    encodedData.push_back(encoded);
    currentOffset += encoded.size();
}