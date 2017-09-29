//
// Created by Davide  Caroselli on 15/05/17.
//

#ifndef MMT_PBDECODER_SENTENCE_H
#define MMT_PBDECODER_SENTENCE_H

#include <cstdint>
#include <vector>
#include <unordered_map>

namespace mmt {

    typedef uint32_t memory_t;
    typedef uint32_t wid_t;
    typedef uint16_t length_t;

    typedef std::vector<wid_t> sentence_t;

    struct cscore_t {
        memory_t memory;
        float score;

        cscore_t(memory_t memory = 0, float score = 0.f) : memory(memory), score(score) {};
    };

    typedef std::vector<cscore_t> context_t;
    typedef std::vector<std::pair<length_t, length_t>> alignment_t;

}
#endif //MMT_PBDECODER_SENTENCE_H
