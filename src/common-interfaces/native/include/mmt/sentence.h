//
// Created by Davide  Caroselli on 31/08/16.
//

#ifndef MMT_COMMON_INTERFACES_SENTENCE_H
#define MMT_COMMON_INTERFACES_SENTENCE_H

#include <cstdint>
#include <vector>
#include <unordered_map>

namespace mmt {

    typedef uint32_t domain_t;
    typedef uint32_t wid_t;
    typedef uint16_t length_t;

    struct word_t {
        wid_t id;

        word_t(wid_t id) : id(id) {};
    };

    typedef std::vector<word_t> sentence_t;

    struct cscore_t {
        domain_t domain;
        float score;

        cscore_t(domain_t domain = 0, float score = 0.f) : domain(domain), score(score) {};
    };

    typedef std::vector<cscore_t> context_t;



    typedef std::pair<length_t, length_t> alignmentPoint_t;
    typedef std::vector<alignmentPoint_t > alignment_t;

    struct alignment_hash {
        size_t operator()(const alignment_t &x) const {
            size_t h = 0;
            for (size_t i = 0; i < x.size(); ++i) {
                alignmentPoint_t align = x[i];

                if (i == 0)
                    h = align.first + 1;
                else
                    h = ((h * 8978948897894561157ULL) ^
                         (static_cast<uint64_t>(1 + align.first) * 17894857484156487943ULL));

                h = ((h * 8978948897894561157ULL) ^
                     (static_cast<uint64_t>(1 + align.second) * 17894857484156487943ULL));
            }

            return h;
        }
    };

}

#endif //MMT_COMMON_INTERFACES_SENTENCE_H
