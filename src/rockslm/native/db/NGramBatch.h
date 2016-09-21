//
// Created by Davide  Caroselli on 07/09/16.
//

#ifndef ROCKSLM_NGRAMBATCH_H
#define ROCKSLM_NGRAMBATCH_H

#include <cstddef>
#include <unordered_map>
#include <vector>
#include <mmt/IncrementalModel.h>
#include "dbkey.h"
#include "counts.h"

using namespace std;

namespace rockslm {
    namespace db {

        struct ngram_t {
            counts_t counts;
            bool is_in_db_for_sure;
            dbkey_t predecessor;

            ngram_t() : counts(), is_in_db_for_sure(false), predecessor(0) {};
        };

        typedef vector<unordered_map<dbkey_t, ngram_t>> ngram_table_t;

        class NGramBatch {
        public:

            NGramBatch(uint8_t order, size_t maxSize) : NGramBatch(order, maxSize, vector<seqid_t>()) {}

            NGramBatch(uint8_t order, size_t maxSize, const vector<seqid_t> &streams);

            inline size_t GetSize() const {
                return size;
            }

            inline size_t GetMaxSize() const {
                return maxSize;
            }

            void Reset(const vector<seqid_t> &streams);

            void Clear() {
                ngrams_map.clear();
            }

            bool Add(const domain_t domain, const vector<wid_t> &sentence, const count_t count = 1);

            bool
            Add(const updateid_t &id, const domain_t domain, const vector<wid_t> &sentence, const count_t count = 1);

            const vector<seqid_t> &GetStreams() const;

            unordered_map<domain_t, ngram_table_t> &GetNGrams();

        private:
            const uint8_t order;
            const size_t maxSize;
            size_t size;

            unordered_map<domain_t, ngram_table_t> ngrams_map;
            vector<seqid_t> streams;

            bool SetStreamIfValid(stream_t stream, seqid_t sentence);

            inline void AddToBatch(const domain_t domain, const vector<wid_t> &sentence, const count_t count = 1);
        };

    }
}


#endif //ROCKSLM_NGRAMBATCH_H
