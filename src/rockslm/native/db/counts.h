//
// Created by Davide  Caroselli on 07/09/16.
//

#ifndef ROCKSLM_COUNTS_H
#define ROCKSLM_COUNTS_H

#include <cstdint>

namespace rockslm {
    namespace db {

        typedef uint32_t count_t;

        struct counts_t {
            count_t count;
            count_t successors;

            counts_t(count_t c = 0, count_t s = 0) : count(c), successors(s) {};
        };

    }
}

#endif //ROCKSLM_COUNTS_H
