//
// Created by Davide  Caroselli on 02/09/16.
//

#ifndef MMT_COMMON_INTERFACES_ALIGNERMODEL_H
#define MMT_COMMON_INTERFACES_ALIGNERMODEL_H

#include <mmt/sentence.h>

using namespace std;

namespace mmt {

    const wid_t kAlignerNullWord = 0;

    class AlignerModel {
    public:

        virtual alignment_t ComputeAlignment(const vector<wid_t> &source, const vector<wid_t> &target) = 0;

        virtual void ComputeAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch, vector<alignment_t> &outAlignments) = 0;

        virtual ~AlignerModel() {};

    };
}


#endif //MMT_COMMON_INTERFACES_ALIGNERMODEL_H
