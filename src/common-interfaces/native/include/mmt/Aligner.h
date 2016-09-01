//
// Created by Davide  Caroselli on 31/08/16.
//

#ifndef MMT_COMMON_INTERFACES_ALIGNER_H
#define MMT_COMMON_INTERFACES_ALIGNER_H

#include "sentence.h"

namespace mmt {

    typedef std::vector<std::pair<length_t, length_t>> alignment_t;

    class Aligner {
    public:

        virtual alignment_t ComputeAlignment(const sentence_t &source, const sentence_t &target) = 0;

        virtual void ComputeAlignments(const std::vector<std::pair<sentence_t, sentence_t>> &batch, std::vector<alignment_t> &outAlignments) = 0;

    };
}


#endif //MMT_COMMON_INTERFACES_ALIGNER_H
