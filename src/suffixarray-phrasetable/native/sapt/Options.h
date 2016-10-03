//
// Created by Davide Caroselli on 27/07/16.
//

#ifndef SAPT_OPTIONS_H
#define SAPT_OPTIONS_H

#include <cstddef>
#include <cstdint>
#include <string>

namespace mmt {
    namespace sapt {

        struct Options {

            // Number of samples used to calculate phrase tables
            size_t samples = 1000;

            // Internal storage prefix max length. This option does not
            // limit the length of searchable prefixes, it is just an
            // internal representation option: higher values will speed
            // up search time while raising the index size.
            uint8_t prefix_length = 5;

            /* Updates */

            // Updates are flushed to disk when one of the following
            // conditions are met: either the buffer size limit is
            // reached, or the maximum delay time has passed.

            // Maximum number of n-grams cached before flushing updates
            // to the underlying database.
            size_t update_buffer_size = 100000; // number of sentence pairs

            // Maximum time in seconds that an update can wait before
            // being flushed to disk; higher delays ensures better
            // throughput thanks to the buffer, but higher values
            // also mean a longer time before an update is visible
            // to the user.
            double update_max_delay = 2.; // seconds


            double numScoreComponent = 4; // number of scores provided by the Phrase Table

            Options() {};
        };

    }
}


#endif //SAPT_OPTIONS_H
