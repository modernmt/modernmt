//
// Created by Davide  Caroselli on 10/10/16.
//

#ifndef SAPT_SAMPLE_H
#define SAPT_SAMPLE_H

#include <vector>
#include <sstream>
#include <mmt/sentence.h>

using namespace std;

namespace mmt {
    namespace sapt {

        struct sample_t {
            memory_t memory;
            vector<wid_t> source;
            vector<wid_t> target;
            alignment_t alignment;
            vector<length_t> offsets;

            string ToString() const {
                ostringstream repr;
                repr << "(" << memory << ")";

                for (auto word = source.begin(); word != source.end(); ++word)
                    repr << " " << *word;
                repr << " |||";
                for (auto word = target.begin(); word != target.end(); ++word)
                    repr << " " << *word;
                repr << " |||";
                for (auto a = alignment.begin(); a != alignment.end(); ++a)
                    repr << " " << a->first << "-" << a->second;
                repr << " ||| offsets:";
                for (auto o = offsets.begin(); o != offsets.end(); ++o)
                    repr << " " << *o;

                return repr.str();
            }
        };

    }
}

#endif //SAPT_SAMPLE_H
