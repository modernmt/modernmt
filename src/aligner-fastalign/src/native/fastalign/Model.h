//
// Created by Davide  Caroselli on 23/08/16.
//

#ifndef FASTALIGN_MODEL_H
#define FASTALIGN_MODEL_H

#include <string>
#include <vector>
#include <unordered_map>
#include <mmt/sentence.h>

namespace mmt {
    namespace fastalign {

        const double kNullProbability = 1e-9;

        class Model {
            friend class Builder;

        public:
            Model(bool is_reverse, bool use_null, bool favor_diagonal, double prob_align_null, double diagonal_tension);

            inline alignment_t ComputeAlignment(const std::vector<wid_t> &source, const std::vector<wid_t> &target) {
                alignment_t alignment;
                ComputeAlignment(source, target, NULL, &alignment);
                return alignment;
            }

            inline void ComputeAlignments(const std::vector<std::pair<std::vector<wid_t>, std::vector<wid_t>>> &batch,
                                          std::vector<alignment_t> &outAlignments) {
                ComputeAlignments(batch, NULL, &outAlignments);
            }

            virtual double GetProbability(wid_t source, wid_t target) = 0;

            virtual void IncrementProbability(wid_t source, wid_t target, double amount) = 0;

            virtual ~Model() {};

        protected:
            const bool is_reverse;
            const bool use_null;
            const bool favor_diagonal;
            const double prob_align_null;

            double diagonal_tension;

            double ComputeAlignment(const std::vector<wid_t> &source, const std::vector<wid_t> &target, Model *outModel,
                                    alignment_t *outAlignment);

            double ComputeAlignments(const std::vector<std::pair<std::vector<wid_t>, std::vector<wid_t>>> &batch,
                                     Model *outModel, std::vector<alignment_t> *outAlignments);
        };

    }
}

#endif //FASTALIGN_MODEL_H
