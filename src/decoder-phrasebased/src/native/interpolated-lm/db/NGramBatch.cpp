//
// Created by Davide  Caroselli on 07/09/16.
//

#include "NGramBatch.h"
#include <stdlib.h>
#include <lm/LM.h>

using namespace mmt;
using namespace mmt::ilm;

NGramBatch::NGramBatch(uint8_t order, size_t maxSize, const vector<seqid_t> &_streams) : order(order), maxSize(maxSize),
                                                                                         size(0), sentenceCount(0) {
    streams = _streams;
}

void NGramBatch::Add(const channel_t channel, const seqid_t position,
                     const memory_t memory, const vector<wid_t> &sentence, const count_t count) {
    if (ShouldAcceptUpdate(channel, position))
        Add(memory, sentence, count);
}

void NGramBatch::Add(const memory_t memory, const vector<wid_t> &sentence, const count_t count) {
    sentenceCount++;

    auto el = ngrams_map.emplace(memory, ngram_table_t());
    ngram_table_t &ngrams = el.first->second;

    if (el.second)
        ngrams.resize(order);

    // Create word array with start and end symbols
    size_t words_length = sentence.size() + 2;
    wid_t *words = (wid_t *) calloc(words_length, sizeof(wid_t));
    std::copy(sentence.begin(), sentence.end(), &words[1]);

    words[0] = kVocabularyStartSymbol;
    words[words_length - 1] = kVocabularyEndSymbol;

    // Fill table
    for (size_t iword = 0; iword < words_length; ++iword) {
        ngram_hash_t key = 0;

        for (size_t iorder = 0; iorder < order; ++iorder) {
            if (iword + iorder >= words_length)
                break;

            wid_t word = words[iword + iorder];
            ngram_hash_t current = iorder == 0 ? word : hash_ngram(key, word);

            auto e = ngrams[iorder].emplace(current, ngram_t());
            ngram_t &ngram = e.first->second;
            ngram.predecessor = key;
            ngram.counts.count += count;

            if (e.second)
                this->size++;

            key = current;
        }
    }

    delete words;
}

void NGramBatch::Delete(const channel_t channel, const seqid_t position, const memory_t memory) {
    if (ShouldAcceptUpdate(channel, position))
        deletions.push_back(memory);
}

void NGramBatch::Reset(const vector<seqid_t> &_streams) {
    streams = _streams;
    Clear();
}

void NGramBatch::Clear() {
    ngrams_map.clear();
    deletions.clear();
    size = 0;
    sentenceCount = 0;
}

const vector<seqid_t> &NGramBatch::GetStreams() const {
    return streams;
}

bool NGramBatch::IsEmpty() {
    return size == 0 && deletions.empty();
}

bool NGramBatch::IsFull() {
    return size >= maxSize;
}

void NGramBatch::Advance(const unordered_map<channel_t, seqid_t> &channels) {
    for (auto entry = channels.begin(); entry != channels.end(); ++entry) {
        channel_t channel = entry->first;
        seqid_t position = entry->second;

        if ((ssize_t) streams.size() <= channel)
            streams.resize((size_t) (channel + 1), -1);

        if (streams[channel] < position)
            streams[channel] = position;
    }
}
