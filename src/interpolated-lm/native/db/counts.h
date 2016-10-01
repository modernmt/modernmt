//
// Created by Davide  Caroselli on 07/09/16.
//

#ifndef ILM_COUNTS_H
#define ILM_COUNTS_H

#include <cstdint>

namespace mmt {
    namespace ilm {

        typedef uint32_t count_t;

        struct counts_t {
            count_t count;
            count_t successors;

            counts_t(count_t c = 0, count_t s = 0) : count(c), successors(s) {};
        };

    }
}

#endif //ILM_COUNTS_H
