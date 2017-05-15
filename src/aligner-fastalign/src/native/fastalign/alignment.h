//
// Created by Davide  Caroselli on 12/05/17.
//

#ifndef MMT_FASTALIGN_ALIGNMENT_H
#define MMT_FASTALIGN_ALIGNMENT_H

#include <cstdint>
#include <vector>
#include <unordered_map>

namespace mmt {
    namespace fastalign {

        typedef uint32_t word_t;
        typedef uint16_t length_t;

        typedef std::vector<std::pair<length_t, length_t>> alignment_t;
        typedef std::vector<std::string> sentence_t;
        typedef std::vector<word_t> wordvec_t;

    }
}

#endif //MMT_FASTALIGN_ALIGNMENT_H
