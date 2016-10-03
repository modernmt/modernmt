//
// Created by Davide  Caroselli on 31/08/16.
//

#ifndef MMT_COMMON_INTERFACES_ALIGNER_H
#define MMT_COMMON_INTERFACES_ALIGNER_H

#include <mmt/sentence.h>
#include <mmt/aligner/AlignerModel.h>

using namespace std;

namespace mmt {

    enum SymmetrizationStrategy {
        GrowDiagonalFinalAndStrategy = 1,
        GrowDiagonalStrategy = 2,
        IntersectionStrategy = 3,
        UnionStrategy = 4
    };

    class Aligner {
    public:

        virtual alignment_t GetAlignment(const vector<wid_t> &source, const vector<wid_t> &target,
                                         SymmetrizationStrategy strategy = GrowDiagonalFinalAndStrategy) = 0;

        virtual alignment_t GetForwardAlignment(const vector<wid_t> &source, const vector<wid_t> &target) = 0;

        virtual alignment_t GetBackwardAlignment(const vector<wid_t> &source, const vector<wid_t> &target) = 0;

        virtual void GetForwardAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch,
                                          vector<alignment_t> &outAlignments) = 0;

        virtual void GetBackwardAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch,
                                           vector<alignment_t> &outAlignments) = 0;

        virtual void
        GetAlignments(const vector<pair<vector<wid_t>, vector<wid_t>>> &batch, vector<alignment_t> &outAlignments,
                      SymmetrizationStrategy strategy = GrowDiagonalFinalAndStrategy) = 0;

        virtual float GetForwardProbability(wid_t source, wid_t target);

        virtual float GetBackwardProbability(wid_t source, wid_t target);

        virtual ~Aligner() {};

    };
}


#endif //MMT_COMMON_INTERFACES_ALIGNER_H
