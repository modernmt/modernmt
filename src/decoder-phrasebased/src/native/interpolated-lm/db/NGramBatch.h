//
// Created by Davide  Caroselli on 07/09/16.
//

#ifndef ILM_NGRAMBATCH_H
#define ILM_NGRAMBATCH_H

#include <cstddef>
#include <unordered_map>
#include <vector>
#include <mmt/IncrementalModel.h>
#include "ngram_hash.h"
#include "counts.h"

using namespace std;

namespace mmt {
    namespace ilm {

        struct ngram_t {
            counts_t counts;
            bool is_in_db_for_sure;
            ngram_hash_t predecessor;

            ngram_t() : counts(), is_in_db_for_sure(false), predecessor(0) {};
        };

        typedef vector<unordered_map<ngram_hash_t, ngram_t>> ngram_table_t;

        class NGramBatch {
            friend class NGramStorage;
        public:

            NGramBatch(uint8_t order, size_t maxSize) : NGramBatch(order, maxSize, vector<seqid_t>()) {}

            NGramBatch(uint8_t order, size_t maxSize, const vector<seqid_t> &streams);

            bool Add(const memory_t memory, const vector<wid_t> &sentence, const count_t count = 1);

            bool Add(const updateid_t &id, const memory_t memory, const vector<wid_t> &sentence,
                     const count_t count = 1);

            bool Delete(const updateid_t &id, const memory_t memory);

            bool IsEmpty();

            void Reset(const vector<seqid_t> &streams);

            void Clear();

            const vector<seqid_t> &GetStreams() const;

        private:
            const uint8_t order;
            const size_t maxSize;
            size_t size;
            size_t sentenceCount;

            vector<seqid_t> streams;
            unordered_map<memory_t, ngram_table_t> ngrams_map;
            vector<memory_t> deletions;

            bool SetStreamIfValid(stream_t stream, seqid_t sentence);

            inline void AddToBatch(const memory_t memory, const vector<wid_t> &sentence, const count_t count = 1);
        };

    }
}


#endif //ILM_NGRAMBATCH_H
