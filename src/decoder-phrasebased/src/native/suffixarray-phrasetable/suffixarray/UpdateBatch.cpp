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

void UpdateBatch::Add(const channel_t channel, const seqid_t position, const memory_t memory,
                      const vector<wid_t> &source, const vector<wid_t> &target, const alignment_t &alignment) {
    if (ShouldAcceptUpdate(channel, position))
        Add(memory, source, target, alignment);
}

void UpdateBatch::Add(const memory_t memory, const std::vector<wid_t> &source,
                      const std::vector<wid_t> &target, const alignment_t &alignment) {
    sentencepair_t pair;
    pair.memory = memory;
    pair.source = source;
    pair.target = target;
    pair.alignment = alignment;

    data.push_back(pair);
}

void UpdateBatch::Delete(const channel_t channel, const seqid_t position, const memory_t memory) {
    if (ShouldAcceptUpdate(channel, position))
        deletions.push_back(memory);
}

bool UpdateBatch::IsEmpty() {
    return data.empty() && deletions.empty();
}

bool UpdateBatch::IsFull() {
    return data.size() >= maxSize;
}

void UpdateBatch::Advance(const unordered_map<channel_t, seqid_t> &channels) {
    for (auto entry = channels.begin(); entry != channels.end(); ++entry) {
        channel_t channel = entry->first;
        seqid_t position = entry->second;

        if (streams.size() <= channel)
            streams.resize((size_t) (channel + 1), -1);

        if (streams[channel] < position)
            streams[channel] = position;
    }
}