//
// Created by Davide  Caroselli on 07/09/16.
//

#ifndef ILM_KEY_H
#define ILM_KEY_H

#include <cstdint>
#include <lm/LM.h>

namespace mmt {
    namespace ilm {

        typedef uint64_t dbkey_t;

        inline dbkey_t make_key(const wid_t word) {
            return (dbkey_t) word;
        }

        inline dbkey_t make_key(const dbkey_t current, const wid_t word) {
            dbkey_t key = ((current * 8978948897894561157ULL) ^
                         (static_cast<uint64_t>(1 + word) * 17894857484156487943ULL));
            return key == 0 ? 1 : key; // key "0" is reserved
        }

        inline dbkey_t make_key(const vector<wid_t> &words, const size_t offset, const size_t size) {
            dbkey_t key = make_key(words[offset]);

            for (size_t i = 1; i < size; ++i)
                key = make_key(key, words[offset + i]);

            return key;
        }

        inline dbkey_t make_key(const vector<wid_t> &words, const size_t order) {
            const size_t offset = (size_t) std::max(0, (int) (words.size() - order));
            return make_key(words, offset, order);
        }

    }

}

#endif //ILM_KEY_H
