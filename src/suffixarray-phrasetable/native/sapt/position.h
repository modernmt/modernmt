//
// Created by Davide  Caroselli on 27/09/16.
//

#ifndef SAPT_POSITION_H
#define SAPT_POSITION_H

#include <cstdint>
#include <mmt/sentence.h>

namespace mmt {
    namespace sapt {

        struct position_t {
            int64_t corpus_offset;
            length_t word_offset;

            position_t(int64_t corpus_offset = 0, length_t word_offset = 0) : corpus_offset(corpus_offset),
                                                                              word_offset(word_offset) {};
        };

    }
}

#endif //SAPT_POSITION_H
