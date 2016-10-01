//
// Created by Roldano Cattoni on 14/09/16.
//

#ifndef ILM_ADAPTIVELMCACHE_H
#define ILM_ADAPTIVELMCACHE_H

#include <string>
#include <unordered_map>
#include <db/dbkey.h>
#include "LM.h"

using namespace std;

namespace mmt {
    namespace ilm {

        typedef uint64_t cachekey_t;

        struct cachevalue_t {
            float probability;  // n-gram probability
            uint8_t length;     // the length of the longest match found in the lm

            cachevalue_t() : probability(0), length(0) {};
        };

        class AdaptiveLMCache {
        public:

            // Empirical estimate of n-grams, suggested values are:
            //  - 100.000 words
            //  - 500.000 2-grams
            //  - 600.000 3-grams
            //  - 600.000 4-grams
            //  - 500.000 5-grams
            AdaptiveLMCache(uint8_t order, size_t initialSize = 2000000) : order(order) {
                cache.reserve(initialSize);
            }

            inline bool IsCacheable(size_t order) {
                return order <= this->order;
            }

            // store in the cache the entry
            //   key(word, state) -> (probability, outStateLength)
            //   if already present, replace the old value with the new value;
            inline void Put(const dbkey_t key, const cachevalue_t &value) {
                cache[key] = value;
            }

            // lookup in the cache the entry key(word, state), if found, fill the given parameters
            // and return true, else return false
            inline bool Get(const dbkey_t key, cachevalue_t *outValue) {
                auto it = cache.find(key);

                if (it == cache.end()) {
                    return false;
                } else {
                    *outValue = it->second;
                    return true;
                }
            }

        private:
            const uint8_t order;
            unordered_map <cachekey_t, cachevalue_t> cache;
        };

    }
}

#endif //ILM_ADAPTIVELMCACHE_H
