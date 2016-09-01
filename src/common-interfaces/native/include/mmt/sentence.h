//
// Created by Davide  Caroselli on 31/08/16.
//

#ifndef MMT_COMMON_INTERFACES_SENTENCE_H
#define MMT_COMMON_INTERFACES_SENTENCE_H

#include <cstdint>
#include <vector>

namespace mmt {

    typedef uint32_t wid_t;

    struct word_t {
        wid_t id;

        static inline word_t make_word(wid_t id) {
            word_t word;
            word.id = id;
            return word;
        }
    };

    typedef std::vector<word_t> sentence_t;
    typedef uint16_t length_t;

}

#endif //MMT_COMMON_INTERFACES_SENTENCE_H
