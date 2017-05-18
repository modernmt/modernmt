//
// Created by Davide  Caroselli on 15/05/17.
//

#ifndef MMT_PBDECODER_SENTENCE_H
#define MMT_PBDECODER_SENTENCE_H

#include <cstdint>
#include <vector>
#include <unordered_map>

namespace mmt {

    typedef uint32_t domain_t;
    typedef uint32_t wid_t;
    typedef uint16_t length_t;

    typedef std::vector<wid_t> sentence_t;

    struct cscore_t {
        domain_t domain;
        float score;

        cscore_t(domain_t domain = 0, float score = 0.f) : domain(domain), score(score) {};
    };

    typedef std::vector<cscore_t> context_t;
    typedef std::vector<std::pair<length_t, length_t>> alignment_t;

}
#endif //MMT_PBDECODER_SENTENCE_H
