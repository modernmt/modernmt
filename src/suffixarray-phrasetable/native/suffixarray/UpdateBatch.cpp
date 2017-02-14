//
// Created by Davide  Caroselli on 27/09/16.
//

#include <iostream>
#include "UpdateBatch.h"

using namespace mmt::sapt;

UpdateBatch::UpdateBatch(size_t maxSize, const vector<seqid_t> &_streams) : maxSize(maxSize) {
    streams = _streams;
    data.reserve(maxSize);
}

void UpdateBatch::Clear() {
    data.clear();
    deletions.clear();
}

void UpdateBatch::Reset(const vector<seqid_t> &_streams) {
    streams = _streams;
    Clear();
}

bool UpdateBatch::SetStreamIfValid(stream_t stream, seqid_t sentence) {
    if ((int) streams.size() <= stream)
        streams.resize(((size_t) stream) + 1, -1);

    if (streams[stream] < sentence) {
        streams[stream] = sentence;
        return true;
    } else {
        return false;
    }
}

bool UpdateBatch::Add(const updateid_t &id, const domain_t domain, const std::vector<wid_t> &source,
                      const std::vector<wid_t> &target, const alignment_t &alignment) {
    if (GetSize() >= GetMaxSize())
        return false;

    if (!SetStreamIfValid(id.stream_id, id.sentence_id))
        return true;

    sentencepair_t pair;
    pair.domain = domain;
    pair.source = source;
    pair.target = target;
    pair.alignment = alignment;

    data.push_back(pair);

    return true;
}

bool UpdateBatch::Add(const domain_t domain, const std::vector<wid_t> &source,
                      const std::vector<wid_t> &target, const alignment_t &alignment) {
    if (GetSize() >= GetMaxSize())
        return false;

    sentencepair_t pair;
    pair.domain = domain;
    pair.source = source;
    pair.target = target;
    pair.alignment = alignment;

    data.push_back(pair);

    return true;
}

bool UpdateBatch::Delete(const mmt::updateid_t &id, const mmt::domain_t domain) {
    if (GetSize() >= GetMaxSize())
        return false;

    if (!SetStreamIfValid(id.stream_id, id.sentence_id))
        return true;

    deletions.push_back(domain);
    return true;
}
