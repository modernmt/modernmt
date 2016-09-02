//
// Created by Davide  Caroselli on 31/08/16.
//

#ifndef MMT_COMMON_INTERFACES_ALIGNER_H
#define MMT_COMMON_INTERFACES_ALIGNER_H

#include <mmt/sentence.h>

using namespace std;

namespace mmt {

    typedef vector<pair<length_t, length_t>> alignment_t;

    class Aligner {
    public:

        virtual alignment_t ComputeAlignment(const vector<wid_t> &source, const vector<wid_t> &target) = 0;

        virtual void ComputeAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch, vector<alignment_t> &outAlignments) = 0;

    };
}


#endif //MMT_COMMON_INTERFACES_ALIGNER_H
