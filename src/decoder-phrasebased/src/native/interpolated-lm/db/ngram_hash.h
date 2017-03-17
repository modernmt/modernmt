//
// Created by Davide  Caroselli on 17/02/17.
//

#ifndef ILM_NGRAM_HASH_H
#define ILM_NGRAM_HASH_H

#include <cstdint>
#include <mmt/sentence.h>

namespace mmt {
    namespace ilm {

        /* N-Grams hash */

        typedef uint64_t ngram_hash_t;

        inline ngram_hash_t hash_ngram(const wid_t word) {
            return (ngram_hash_t) word;
        }

        inline ngram_hash_t hash_ngram(const ngram_hash_t current, const wid_t word) {
            ngram_hash_t key = ((current * 8978948897894561157ULL) ^
                                (static_cast<uint64_t>(1 + word) * 17894857484156487943ULL));
            return key == 0 ? 1 : key; // key "0" is reserved
        }

        inline ngram_hash_t hash_ngram(const std::vector<wid_t> &words, const size_t offset, const size_t size) {
            ngram_hash_t key = hash_ngram(words[offset]);

            for (size_t i = 1; i < size; ++i)
                key = hash_ngram(key, words[offset + i]);

            return key;
        }

        inline ngram_hash_t hash_ngram(const std::vector<wid_t> &words, const size_t order) {
            const size_t offset = (size_t) std::max(0, (int) (words.size() - order));
            return hash_ngram(words, offset, order);
        }
    }
}

#endif //ILM_NGRAM_HASH_H
