//
// Created by Davide  Caroselli on 31/08/16.
//

#ifndef MMT_COMMON_INTERFACES_SENTENCE_H
#define MMT_COMMON_INTERFACES_SENTENCE_H

#include <cstdint>
#include <vector>

namespace mmt {

    typedef uint32_t wid_t;
    typedef uint16_t length_t;

    struct word_t {
        wid_t id;

        word_t(wid_t id) : id(id) {};
    };

    typedef std::vector<word_t> sentence_t;
    typedef std::vector<std::pair<length_t, length_t>> alignment_t;

}

#endif //MMT_COMMON_INTERFACES_SENTENCE_H
