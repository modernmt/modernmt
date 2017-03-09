//
// Created by Davide  Caroselli on 31/08/16.
//

#ifndef MMT_COMMON_INTERFACES_ALIGNER_H
#define MMT_COMMON_INTERFACES_ALIGNER_H

#include <mmt/sentence.h>

namespace mmt {

    const wid_t kAlignerNullWord = 0;

    enum SymmetrizationStrategy {
        GrowDiagonalFinalAndStrategy = 1,
        GrowDiagonalStrategy = 2,
        IntersectionStrategy = 3,
        UnionStrategy = 4
    };

    class Aligner {
    public:

        virtual alignment_t GetAlignment(const std::vector<wid_t> &source, const std::vector<wid_t> &target,
                                         SymmetrizationStrategy strategy = GrowDiagonalFinalAndStrategy) = 0;

        virtual alignment_t GetForwardAlignment(const std::vector<wid_t> &source,
                                                const std::vector<wid_t> &target) = 0;

        virtual alignment_t GetBackwardAlignment(const std::vector<wid_t> &source,
                                                 const std::vector<wid_t> &target) = 0;

        virtual void
        GetForwardAlignments(const std::vector<std::pair<std::vector<wid_t>, std::vector<wid_t>>> &batch,
                             std::vector<alignment_t> &outAlignments) = 0;

        virtual void
        GetBackwardAlignments(const std::vector<std::pair<std::vector<wid_t>, std::vector<wid_t>>> &batch,
                              std::vector<alignment_t> &outAlignments) = 0;

        virtual void
        GetAlignments(const std::vector<std::pair<std::vector<wid_t>, std::vector<wid_t>>> &batch,
                      std::vector<alignment_t> &outAlignments,
                      SymmetrizationStrategy strategy = GrowDiagonalFinalAndStrategy) = 0;

        // P(target | source)
        virtual float GetForwardProbability(wid_t source, wid_t target) = 0;

        // P(source | target)
        virtual float GetBackwardProbability(wid_t source, wid_t target) = 0;

        // P(NULL | source)
        virtual float GetSourceNullProbability(wid_t source) = 0;

        // P(NULL | target)
        virtual float GetTargetNullProbability(wid_t target) = 0;

        virtual ~Aligner() {};

    };
}


#endif //MMT_COMMON_INTERFACES_ALIGNER_H
