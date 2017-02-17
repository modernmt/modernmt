//
// Created by Davide Caroselli on 27/07/16.
//

#ifndef ILM_OPTIONS_H
#define ILM_OPTIONS_H

#include <cstddef>
#include <cstdint>
#include <string>

namespace mmt {
    namespace ilm {

        struct Options {

            // N-Gram order of the Language Model
            uint8_t order = 5;

            // This value controls the balance between the probabilities
            // returned by the static lm and the adaptive lm.
            // A value of 0 forces the n-gram probability to be exactly
            // the same of the static lm.
            float adaptivity_ratio = .5f;

            /* Updates */

            // Updates are flushed to disk when one of the following
            // conditions are met: either the buffer size limit is
            // reached, or the maximum delay time has passed.

            // Maximum number of n-grams cached before flushing updates
            // to the underlying database.
            size_t update_buffer_size = 100000; // number of n-grams

            // Maximum time in seconds that an update can wait before
            // being flushed to disk; higher delays ensures better
            // throughput thanks to the buffer, but higher values
            // also mean a longer time before an update is visible
            // to the user.
            double update_max_delay = 2.; // seconds

            /* Garbage Collector */

            // Time in seconds between Garbage Collector activations
            double gc_timeout = 12.; // seconds

            Options() {};
        };

    }
}


#endif //ILM_OPTIONS_H
